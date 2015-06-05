#!/usr/bin/env python
# -*- coding: utf-8 -*-
# vim: ai ts=4 sts=4 et sw=4 nu

from __future__ import (unicode_literals, absolute_import,
                        division, print_function)
import sys
import os
import copy
import json
import shutil
import logging
import StringIO
import tempfile
import urllib2
from subprocess import call

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

# the directory of this file for relative referencing
CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))

# the parent directory of this file for relative referencing
PARENT_PATH = os.path.dirname(CURRENT_PATH)

ANDROID_PATH = tempfile.mkdtemp(prefix='android-custom-', dir=PARENT_PATH)

DEFAULT_JSDATA = {
    # mandatory fields
    # 'app_name': "Kiwix Custom App",
    # 'package': "org.kiwix.zim.custom",
    # 'version_name': "1.0",
    # 'zim_file': "wikipedia_bm.zim",
    # 'license': None,

    # main icon source & store icon
    'ic_launcher': os.path.join('android',
                                'Kiwix_icon_transparent_512x512.png'),

    # store listing
    'feature_image': None,
    'phone_screenshot': None,
    'tablet7_screenshot': None,
    'tablet10_screenshot': None,
    'category': '',
    'rating': 'everyone',
    'website': "http://kiwix.org",
    'email': "kelson@kiwix.org",

    # help page content
    'support_email': "kelson@kiwix.org",

    'enforced_lang': None
}

SIZE_MATRIX = {
    'xhdpi': 96,
    'mdpi': 72,
    'ldpi': 48,
    'hdpi': 72,
}

PERMISSIONS = [
    'com.android.vending.CHECK_LICENSE',  # access Google Play Licensing
    'android.permission.WAKE_LOCK',  # keep CPU alive while downloading files
    'android.permission.ACCESS_WIFI_STATE'  # check whether Wi-Fi is enabled
]

# external dependencies (make sure we're all set up!)
try:
    import requests
    from bs4 import BeautifulSoup
    # check for convert (imagemagick)
except ImportError:
    logger.error("Missing dependency: Unable to import requests.\n"
                 "Please install requests with "
                 "`pip install requests BeautifulSoup4 lxml` "
                 "either on your machine or in a virtualenv.")
    sys.exit(1)

# JSON fields that are mandatory to build
required_fields = ('app_name', 'package', 'version_name', 'version_code',
                   'zim_file')


def usage(arg0, exit=None):
    print("Usage: {} <json_file>".format(arg0))
    if exit is not None:
        sys.exit(exit)


def syscall(args, shell=False, with_print=True):
    ''' make a system call '''
    args = args.split()
    if with_print:
        print(u"-----------\n" + u" ".join(args) + u"\n-----------")

    if shell:
        args = ' '.join(args)
    call(args, shell=shell)


def get_remote_content(url):
    req = requests.get(url)
    try:
        req.raise_for_status()
    except Exception as e:
        logger.error("Failed to load data at `{}`".format(url))
        logger.exception(e)
        sys.exit(1)
    return StringIO.StringIO(req.text)


def get_local_content(path):
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
    if is_remote_path(path):
        return get_remote_content(path)
    else:
        return get_local_content(path)


def copy_to(src, dst):
    if is_remote_path(src):
        local = tempfile.NamedTemporaryFile(delete=False)
        local.write(get_remote_content(src))
        local.close()
        src = local
    shutil.copy(src, dst)


def get_remote_url_size(url):
    try:
        return int(urllib2.urlopen(url).info().getheaders("Content-Length")[0])
    except:
        return None


def download_remote_file(url, path):
    req = requests.get(url)
    req.raise_for_status()
    with open(path, 'w') as f:
        f.write(req.text)


def get_file_size(path):
    if is_remote_path(path):
        url = path
        size = get_remote_url_size(url)
        if size is not None:
            return size
        path = "fetched-zim_{}".format(url.rsplit('/', 1))
        download_remote_file(url, path)
    return os.stat(path).st_size


def flushxml(dom, rootNodeName, fpath, head=True):
    head = '<?xml version="1.0" encoding="utf-8"?>\n' if head else ''
    with open(fpath, 'w') as f:
        f.write("{head}{content}"
                .format(head=head,
                        content=dom.find(rootNodeName).encode()))


def main(args):

    # ensure we were provided a Json argument
    if len(args) < 2:
        usage(args[0], 1)

    jspath = args[1]

    fd = get_local_remote_fd(jspath)

    # parse the json file
    jsdata = copy.copy(DEFAULT_JSDATA)
    try:
        jsdata.update(json.load(fd))
    except Exception as e:
        logger.error("Unable to parse JSON file `{}`. Might be malformed."
                     .format(jspath))
        logger.exception(e)
        sys.exit(1)

    # ensure required properties are present
    for key in required_fields:
        if key not in jsdata.keys():
            logger.error("Required field `{}` is missing from JSON file."
                         .format(key))
            logger.error("Required fields are: {}"
                         .format(", ".join(required_fields)))
            sys.exit(1)

    # ensure ZIM file is present and find file size
    jsdata.update({'zim_size': str(get_file_size(jsdata.get('zim_file')))})

    # greetings
    logger.info("Your are now building {app_name} version {version_name} "
                "at {path}"
                .format(app_name=jsdata.get('app_name'),
                        version_name=jsdata.get('version_name'),
                        path=ANDROID_PATH))

    # move to PARENT_PATH (Kiwix main root) to avoid relative remove hell
    os.chdir(PARENT_PATH)

    # remove android folder if exists
    shutil.rmtree(ANDROID_PATH)

    # copy the whole android tree
    shutil.copytree(os.path.join(PARENT_PATH, 'android'),
                    ANDROID_PATH, symlinks=True)

    # move to the newly-created android tree
    os.chdir(ANDROID_PATH)

    # copy launcher icons
    copy_to(jsdata.get('ic_launcher'), os.path.join(ANDROID_PATH,
                                                    'ic_launcher_512.png'))
    # create multiple size icons
    for density, pixels in SIZE_MATRIX.items():
        syscall("convert {inf} -resize {p}x{p} {outf}"
                .format(inf=os.path.join(ANDROID_PATH, 'ic_launcher_512.png'),
                        p=pixels,
                        outf=os.path.join(ANDROID_PATH, 'res',
                                          'drawable-{}'.format(density),
                                          'kiwix_icon.png')))

    # copy and rewrite res/values/branding.xml
    branding_xml = os.path.join(ANDROID_PATH, 'res', 'values', 'branding.xml')
    soup = soup = BeautifulSoup(open(branding_xml, 'r'),
                                'xml', from_encoding='utf-8')
    for elem in soup.findAll('string'):
        elem.string.replace_with(
            elem.text.replace('Kiwix', jsdata.get('app_name')))
    flushxml(soup, 'resources', branding_xml)

    # copy and rewrite src/org/kiwix/kiwimobile/settings/Constants.java
    shutil.copy(os.path.join(ANDROID_PATH, 'templates', 'Constants.java'),
                os.path.join(ANDROID_PATH, 'src', 'org', 'kiwix',
                             'kiwixmobile', 'settings', 'Constants.java'))
    cpath = os.path.join(ANDROID_PATH, 'src', 'org', 'kiwix',
                         'kiwixmobile', 'settings', 'Constants.java')
    content = open(cpath, 'r').read()
    for key, value in jsdata.items():
        content = content.replace('~{key}~'.format(key=key), value or '')
    with open(cpath, 'w') as f:
        f.write(content)

    # Parse and edit res/menu/main.xml
    menu_xml = os.path.join(ANDROID_PATH, 'res', 'menu', 'main.xml')
    soup = soup = BeautifulSoup(open(menu_xml, 'r'),
                                'xml', from_encoding='utf-8')
    for elem in soup.findAll('item'):
        if elem.get('android:id') == '@+id/menu_openfile':
            elem['android:showAsAction'] = "never"
            elem['android:visible'] = "false"
    flushxml(soup, 'menu', menu_xml, head=False)

    # Parse and edit AndroidManifest.xml
    manif_xml = os.path.join(ANDROID_PATH, 'AndroidManifest.xml')
    soup = soup = BeautifulSoup(open(manif_xml, 'r'),
                                'xml', from_encoding='utf-8')
    # change package
    manifest = soup.find('manifest')
    manifest['package'] = jsdata.get('package')
    # change versionCode & versionName
    manifest['android:versionCode'] = jsdata.get('version_code')
    manifest['android:versionName'] = jsdata.get('version_name')

    # remove file opening intents
    for intent in soup.findAll('intent-filter'):
        if not intent.find("action")['android:name'].endswith('.VIEW'):
            # only remove VIEW intents (keep LAUNCHER and GET_CONTENT)
            continue
        intent.replace_with('')
    flushxml(soup, 'manifest', manif_xml)
    # move kiwixmobile to proper package name
    package_tail = jsdata.get('package').split('.')[-1]
    shutil.move(
        os.path.join(ANDROID_PATH, 'src', 'org', 'kiwix', 'kiwixmobile'),
        os.path.join(ANDROID_PATH, 'src', 'org', 'kiwix', package_tail))
    # replace package in every file
    for dirpath, dirnames, filenames in os.walk(ANDROID_PATH):
        for filename in filenames:
            if filename.endswith('.java') or \
                    filename in ('AndroidManifest.xml', 'main.xml'):
                fpath = os.path.join(dirpath, filename)
                content = open(fpath, 'r').read()
                with open(fpath, 'w') as f:
                    f.write(
                        content.replace('org.kiwix.kiwixmobile',
                                        jsdata.get('package'))
                               .replace('org.kiwix.zim.base',
                                        'org.kiwix.zim.{}'
                                        .format(package_tail)))
    # rewrite kiwix.c for JNI
    fpath = os.path.join(ANDROID_PATH, 'kiwix.c')
    content = open(fpath, 'r').read()
    with open(fpath, 'w') as f:
        f.write(content.replace('org_kiwix_kiwixmobile',
                                "_".join(jsdata.get('package').split('.'))))

    # compile KiwixAndroid
    syscall('./build-android-with-native.py '
            '--toolchain '
            '--lzma '
            '--icu '
            '--zim '
            '--kiwix '
            '--strip '
            '--locales '
            '--apk '
            '--clean '
            '--package={}'
            .format(jsdata.get('package')))  # --apk --clean')

    # move generated APK to satisfy other scripts
    for variant in ('debug', 'debug-unaligned', 'release-unsigned'):
        shutil.move(os.path.join(ANDROID_PATH, 'build', 'outputs', 'apk',
                                 "{}-{}.apk"
                                 .format(jsdata.get('package'), variant)),
                    os.path.join(CURRENT_PATH, 'build', 'outputs', 'apk',
                                 "{}-{}.apk"
                                 .format(jsdata.get('package'), variant)))

    # delete temp folder
    shutil.rmtree(ANDROID_PATH)

if __name__ == '__main__':
    main(sys.argv)
