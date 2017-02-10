#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# vim: ai ts=4 sts=4 et sw=4 nu

from __future__ import (unicode_literals, absolute_import,
                        division, print_function)
import logging
import sys
import os
import json
import requests
import tempfile
import shutil
import codecs
from subprocess import call
try:
    from StringIO import StringIO
except ImportError:
    from io import StringIO

# check for python version as google client api is broken on py2
if sys.version_info.major < 3:
    print("You must run this script with python3 as "
          "Google API Client is broken python2")
    sys.exit(1)

PLAY_STORE = 'play_store'
ALPHA = 'alpha'
BETA = 'beta'
PROD = 'production'

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)
for handler in logging.root.handlers:
    handler.addFilter(logging.Filter('__main__'))
CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))


def usage(arg0, exit=None):
    print("Usage: {} <json_file>".format(arg0))
    if exit is not None:
        sys.exit(exit)


def syscall(args, shell=False, with_print=True):
    ''' execute an external command. Use shell=True if using bash specifics '''
    args = args.split()
    if with_print:
        print(u"-----------\n" + u" ".join(args) + u"\n-----------")

    if shell:
        args = ' '.join(args)
    call(args, shell=shell)

def move_to_current_folder():
    os.chdir(CURRENT_PATH)

def get_remote_content(url):
    ''' file descriptor from remote file using GET '''
    req = requests.get(url)
    try:
        req.raise_for_status()
    except Exception as e:
        logger.error("Failed to load data at `{}`".format(url))
        logger.exception(e)
        sys.exit(1)
    return StringIO.StringIO(req.text)


def get_local_content(path):
    ''' file descriptor from local file '''
    if not os.path.exists(path) or not os.path.isfile(path):
        logger.error("Unable to find JSON file `{}`".format(path))
        sys.exit(1)

    try:
        fd = open(path, 'r')
    except Exception as e:
        logger.error("Unable to open file `{}`".format(path))
        logger.exception(e)
        sys.exit(1)
    return fd


def is_remote_path(path):
    return path.startswith('http:')


def get_local_remote_fd(path):
    ''' file descriptor for a path (either local or remote) '''
    if is_remote_path(path):
        return get_remote_content(path)
    else:
        return get_local_content(path)


def copy_to(src, dst):
    ''' copy source content (local or remote) to local file '''
    local = None
    if is_remote_path(src):
        local = tempfile.NamedTemporaryFile(delete=False)
        download_remote_file(src, local.name)
        src = local.name
    shutil.copy(src, dst)
    if local is not None:
        os.remove(local.name)


def download_remote_file(url, path):
    ''' download url to path '''
    syscall('wget -c -O {path} {url}'.format(path=path, url=url))


def upload_to_play_store(jsdata, channel=None):
    if channel is None:
        channel = BETA

    logger.info("Starting Google Play Store using {}".format(channel))

    # ensure dependencies are met
    try:
        import httplib2
        from apiclient.discovery import build
        from oauth2client import client
        from oauth2client.service_account import ServiceAccountCredentials
    except ImportError as error:
        logger.error("You don't have module {0} installed".format(error))
        logger.error("Missing Google API Client dependency.\n"
                     "Please install with: \n"
                     "apt-get install libffi-dev libssl-dev python3-pip\n"
                     "pip3 install google-api-python-client PyOpenSSL\n"
                     "Install from github in case of oauth http errors.")
        return

    if 'GOOGLE_API_KEY' not in os.environ:
        logger.error("You need to set the GOOGLE_API_KEY environment variable "
                     "to use the Google API (using path to google-api.p12)")
        return

    GOOGLE_CLIENT_ID = '107823297044-nhoqv99cpr86vlfcronskirgib2g7tq' \
                       '9@developer.gserviceaccount.com'

    service = build('androidpublisher', 'v2')
    scope = 'https://www.googleapis.com/auth/androidpublisher'
    key = os.environ['GOOGLE_API_KEY']
    credentials = ServiceAccountCredentials.from_p12_keyfile(
        GOOGLE_CLIENT_ID,
        key,
        scopes=[scope])

    http = httplib2.Http()
    http = credentials.authorize(http)

    service = build('androidpublisher', 'v2', http=http)

    package_name = jsdata['package']
    version_name = jsdata['version_name']
    apk_file = os.path.join(CURRENT_PATH, 'build', 'outputs', 'apk',
                            '{}-{}.apk'.format(package_name, version_name))

    json_file_dir = os.path.abspath(os.path.dirname(jspath))

    # download remote zim file
    if is_remote_path(jsdata.get('zim_file')):
        zimfile_url = jsdata.get('zim_file')
        remote_filename = get_remote_file_name(zimfile_url)
        local_file_path = os.path.join(json_file_dir, remote_filename)
        download_remote_file(zimfile_url, local_file_path)
        jsdata.update({'zim_file': local_file_path})

    # update relative paths to absolute
    os.chdir(json_file_dir)
    jsdata.update({'zim_file': os.path.abspath(jsdata.get('zim_file'))})
    move_to_current_folder()

    if not jsdata.get('embed_zim', False):
        comp_file = tempfile.NamedTemporaryFile(suffix='.a').name
        copy_to(jsdata['zim_file'], comp_file)

    try:
        # another edit request
        edit_request = service.edits().insert(body={},
                                              packageName=package_name)
        result = edit_request.execute()
        edit_id = result['id']

        logger.info("Starting Edit `{}`".format(edit_id))

        # upload APK
        logger.info("Uploading APK file: {}".format(apk_file))
        apk_response = service.edits().apks().upload(
            editId=edit_id,
            packageName=package_name,
            media_body=apk_file).execute()

        logger.debug("APK for version code {} has been uploaded"
                     .format(apk_response['versionCode']))

        # release APK into the specified channel
        track_response = service.edits().tracks().update(
            editId=edit_id,
            track=channel,
            packageName=package_name,
            body={'versionCodes': [apk_response['versionCode']]}).execute()

        logger.debug("Publication set to {} for version code {}"
                     .format(track_response['track'],
                             str(track_response['versionCodes'])))

        # upload companion file
        if comp_file:
            logger.info("Uploading Expansion file: {}".format(comp_file))
            comp_response = service.edits().expansionfiles().upload(
                editId=edit_id,
                packageName=package_name,
                apkVersionCode=jsdata['version_code'],
                expansionFileType='main',
                media_body=comp_file).execute()

            logger.debug("Expansion file of size {} has been uploaded"
                         .format(comp_response['expansionFile']['fileSize']))

        commit_request = service.edits().commit(
            editId=edit_id, packageName=package_name).execute()

        logger.debug("Edit `{}` has been committed. done."
                     .format(commit_request['id']))

    except client.AccessTokenRefreshError:
        logger.error("The credentials have been revoked or expired, "
                     "please re-run the application to re-authorize")

STORES = {
    'play_store': upload_to_play_store,
}


def main(json_path, store='{}:{}'.format(PLAY_STORE, ALPHA), *args):
    jsdata = json.load(get_local_remote_fd(json_path))

    logger.info("Uploading {} APK to {}".format(jsdata['package'], store))

    try:
        store, channel = store.split(':', 1)
    except (IndexError, ValueError):
        channel = None

    STORES.get(store)(jsdata, channel=channel)

if __name__ == '__main__':
    # ensure we were provided a JSON file as first argument
    if len(sys.argv) < 2:
        usage(sys.argv[0], 1)
    else:
        jspath = sys.argv[1]
        args = sys.argv[2:]

    main(jspath, *args)
