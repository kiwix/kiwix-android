#!/usr/bin/env python
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

try:
    import httplib2
    from apiclient.discovery import build
    from oauth2client import client
except ImportError:
    print("Missing Google API Client dependency.\n"
          "Please install with: \n"
          "apt-get install libffi-dev libssl-dev\n"
          "pip install google-api-python-client PyOpenSSL\n"
          "Install from github in case of oauth http errors.")
    sys.exit(1)

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


def main(json_path, *args):
    jsdata = json.load(get_local_remote_fd(json_path))

    logger.info("Updating Play Store Content for {}".format(jsdata['package']))

    if not jsdata.get('play_store'):
        logger.error("You have no data in the play_store container")
        sys.exit(1)

    if 'GOOGLE_API_KEY' not in os.environ:
        logger.error("You need to set the GOOGLE_API_KEY environment variable "
                     "to use the Google API (using path to google-api.p12)")
        return

    GOOGLE_CLIENT_ID = '107823297044-nhoqv99cpr86vlfcronskirgib2g7tq' \
                       '9@developer.gserviceaccount.com'

    service = build('androidpublisher', 'v2')

    key = open(os.environ['GOOGLE_API_KEY'], 'rb').read()
    credentials = client.SignedJwtAssertionCredentials(
        GOOGLE_CLIENT_ID,
        key,
        scope='https://www.googleapis.com/auth/androidpublisher')

    http = httplib2.Http()
    http = credentials.authorize(http)

    service = build('androidpublisher', 'v2', http=http)

    package_name = jsdata['package']
    ps = jsdata.get('play_store')
    default_lang = None  # for images
    files_to_delete = []

    try:
        # another edit request
        edit_request = service.edits().insert(body={},
                                              packageName=package_name)
        result = edit_request.execute()
        edit_id = result['id']

        logger.info("Starting Edit #{} for all updates…".format(edit_id))

        if 'details' in ps:
            logger.debug("Updating details")

            details_fields = ['contactEmail', 'contactPhone',
                              'contactWebsite', 'defaultLanguage']

            details_body = {k: v for k, v in ps['details'].items()
                            if k in details_fields and v is not None}

            details_upd = service.edits().details().update(
                editId=edit_id,
                packageName=package_name,
                body=details_body).execute()

            logger.debug("updated with {} items.".format(len(details_upd)))

            # update default_lang with the value we just submitted
            default_lang = details_body.get('defaultLanguage', None)

        if 'listings' in ps:
            logger.debug("Updating listings (main texts)")

            for lang in ps['listings']:

                details_fields = ['fullDescription', 'shortDescription',
                                  'title', 'video']

                details_body = {k: v for k, v in ps['listings'][lang].items()
                                if k in details_fields and v is not None}

                listing_upd = service.edits().listings().update(
                    editId=edit_id,
                    packageName=package_name,
                    language=lang,
                    body=details_body).execute()

                logger.debug("updated {} with {} items"
                             .format(lang, len(listing_upd)))

        if 'images' in ps:
            logger.debug("Updating images")

            # retrieve default language as important for images
            if default_lang is None:
                details_data = service.edits().details().get(
                    editId=edit_id, packageName=package_name,).execute()

                default_lang = details_data['defaultLanguage']

            # upload images to default lang
            for image_type, images in ps['images'].items():
                if not images:
                    continue

                # delete images for that type
                delete_upd = service.edits().images().deleteall(
                    editId=edit_id,
                    packageName=package_name,
                    imageType=image_type,
                    language=default_lang).execute()
                logger.debug("Cleared {} images: {}".format(
                    image_type,
                    ",".join([d['sha1'] for d in delete_upd['deleted']])))

                for image in images:
                    img_file = tempfile.NamedTemporaryFile(suffix='.png').name
                    copy_to(image, img_file)
                    files_to_delete.append(img_file)

                    img_upd = service.edits().images().upload(
                        editId=edit_id,
                        packageName=package_name,
                        imageType=image_type,
                        language=default_lang,
                        media_body=img_file).execute()

                    logger.debug("Uploaded image for {}. sha1: {}"
                                 .format(image_type, img_upd['image']['sha1']))

        # commit *all* the changes on the Play Store
        commit_request = service.edits().commit(
            editId=edit_id, packageName=package_name).execute()

        logger.debug("Edit `{}` has been committed. done."
                     .format(commit_request['id']))

    except client.AccessTokenRefreshError:
        logger.error("The credentials have been revoked or expired, "
                     "please re-run the application to re-authorize")
    finally:
        for f in files_to_delete:
            os.remove(f)

if __name__ == '__main__':
    # ensure we were provided a JSON file as first argument
    if len(sys.argv) < 2:
        usage(sys.argv[0], 1)
    else:
        jspath = sys.argv[1]
        args = sys.argv[2:]

    main(jspath, *args)
