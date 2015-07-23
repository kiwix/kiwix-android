#!/usr/bin/env python
# -*- coding: utf-8 -*-
# vim: ai ts=4 sts=4 et sw=4 nu

''' Generate a custom build of Kiwix for Android working with a single content

    The generated App either embed the ZIM file inside (creating large APKs)
    or is prepared to make use of a Play Store comapnion file.

    APKs uploaded to Play Store are limited to 50MB in size and can have
    up to 2 comapnion files of 2GB each.
    Note: multiple companion files is not supported currently
        ~~ needs update to the libzim.
    The companion file is stored (by the Play Store) on the SD card.

    Large APKs can be distributed outside the Play Store.
    Note that the larger the APK, the longer it takes to install.
    Also, APKs are downloaded then extracted to the *internal* storage
    of the device unless the user specificaly change its settings to
    install to SD card.

    Standard usage is to launch the script with a single JSON file as argument.
    Take a look at JSDATA sample in this script's source code for
    required and optional values to include. '''

from __future__ import (unicode_literals, absolute_import,
                        division, print_function)
import sys
import os
import re
import copy
import json
import shutil
import logging
import StringIO
import tempfile
import urllib2
from collections import OrderedDict
from subprocess import call
PY3 = sys.version_info.major >= 3

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

DEFAULT_JSDATA = {
    # mandatory fields
    # 'app_name': "Kiwix Custom App",
    # 'package': "org.kiwix.zim.custom",
    # 'version_name': "1.0",
    # 'zim_file': "wikipedia_bm.zim",
    'enforced_lang': None,
    'embed_zim': False,

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
}

SIZE_MATRIX = {
    'xxxhdpi': 192,
    'xxhdpi': 144,
    'xhdpi': 96,
    'mdpi': 72,
    'hdpi': 72,
}

# JSON fields that are mandatory to build
REQUIRED_FIELDS = ('app_name', 'package', 'version_name', 'version_code',
                   'zim_file')

USELESS_PERMISSIONS = ['WRITE_EXTERNAL_STORAGE',
                       'INTERNET', 'ACCESS_NETWORK_STATE']

# the directory of this file for relative referencing
CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))

# the parent directory of this file for relative referencing
PARENT_PATH = os.path.dirname(CURRENT_PATH)

ANDROID_PATH = tempfile.mkdtemp(prefix='android-custom-', dir=PARENT_PATH)

# external dependencies (make sure we're all set up!)
try:
    import requests
    from bs4 import BeautifulSoup
except ImportError:
    logger.error("Missing dependency: Unable to import requests "
                 "or beautifulsoup.\n"
                 "Please install requests/beautifulsoup with "
                 "`pip install requests BeautifulSoup4 lxml` "
                 "either on your machine or in a virtualenv.")
    sys.exit(1)


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


def get_remote_url_size(url):
    try:
        return int(urllib2.urlopen(url).info().getheaders("Content-Length")[0])
    except:
        return None


def get_file_size(path):
    ''' file size in bytes of a path (either remote or local) '''
    if is_remote_path(path):
        url = path
        size = get_remote_url_size(url)
        if size is not None:
            return size
        path = "fetched-zim_{}".format(url.rsplit('/', 1))
        download_remote_file(url, path)
    return os.stat(path).st_size


def flushxml(dom, rootNodeName, fpath, head=True):
    ''' write back XML from a BeautifulSoup DOM and root element '''
    head = '<?xml version="1.0" encoding="utf-8"?>\n' if head else ''
    with open(fpath, 'w') as f:
        f.write("{head}{content}"
                .format(head=head,
                        content=dom.find(rootNodeName).encode()
                                                      .decode('utf-8'))
                .encode('utf-8'))


def move_to_android_placeholder():
    os.chdir(ANDROID_PATH)


def move_to_current_folder():
    os.chdir(CURRENT_PATH)


def step_create_android_placeholder(jsdata, **options):
    ''' copy the android source tree in a different place (yet level equiv.)'''

    # move to PARENT_PATH (Kiwix main root) to avoid relative remove hell
    os.chdir(PARENT_PATH)

    # remove android folder if exists
    shutil.rmtree(ANDROID_PATH)

    # copy the whole android tree
    shutil.copytree(os.path.join(PARENT_PATH, 'android'),
                    ANDROID_PATH, symlinks=True)


def step_prepare_launcher_icons(jsdata, **options):
    ''' generate all-sizes icons from the 512 provided one '''

    move_to_android_placeholder()

    copy_to(jsdata.get('ic_launcher'), os.path.join(ANDROID_PATH,
                                                    'ic_launcher_512.png'))
    # create multiple size icons
    for density, pixels in SIZE_MATRIX.items():
        syscall("convert {inf} -resize {p}x{p} {outf}"
                .format(inf=os.path.join(ANDROID_PATH, 'ic_launcher_512.png'),
                        p=pixels,
                        outf=os.path.join(ANDROID_PATH, 'res',
                                          'mipmap-{}'.format(density),
                                          'kiwix_icon.png')))


def step_update_branding_xml(jsdata, **options):
    ''' change app_name value in branding.xml '''

    move_to_android_placeholder()

    # copy and rewrite res/values/branding.xml
    branding_xml = os.path.join(ANDROID_PATH, 'res', 'values', 'branding.xml')
    soup = soup = BeautifulSoup(open(branding_xml, 'r'),
                                'xml', from_encoding='utf-8')
    for elem in soup.findAll('string'):
        elem.string.replace_with(
            elem.text.replace('Kiwix', jsdata.get('app_name')))
    flushxml(soup, 'resources', branding_xml)

    # remove all non-default branding.xml files
    for folder in os.listdir(os.path.join(ANDROID_PATH, 'res')):
        if re.match(r'^values\-[a-z]{2}$', folder):
            lbx = os.path.join(ANDROID_PATH, 'res', folder, 'branding.xml')
            if os.path.exists(lbx):
                os.remove(lbx)


def step_gen_constants_java(jsdata, **options):
    ''' gen Java Source class (final constants) with all JSON values '''

    move_to_android_placeholder()

    # copy and rewrite src/org/kiwix/kiwimobile/settings/Constants.java
    def value_cleaner(val):
        if val is None:
            return ""
        if isinstance(val, bool):
            return str(val).lower()
        if PY3:
            return str(val)
        else:
            return unicode(val)

    # copy template to actual location
    shutil.copy(os.path.join(ANDROID_PATH, 'templates', 'Constants.java'),
                os.path.join(ANDROID_PATH, 'src', 'org', 'kiwix',
                             'kiwixmobile', 'settings', 'Constants.java'))
    cpath = os.path.join(ANDROID_PATH, 'src', 'org', 'kiwix',
                         'kiwixmobile', 'settings', 'Constants.java')
    content = open(cpath, 'r').read()

    # loop through JSON file keys are replace all values
    for key, value in jsdata.items():
        content = content.replace('~{key}~'.format(key=key),
                                  value_cleaner(value))
    with open(cpath, 'w') as f:
        f.write(content)


def step_update_main_menu_xml(jsdata, **options):
    ''' remove Open File menu item from main menu '''

    move_to_android_placeholder()

    # Parse and edit res/menu/main.xml
    menu_xml = os.path.join(ANDROID_PATH, 'res', 'menu', 'menu_main.xml')
    soup = soup = BeautifulSoup(open(menu_xml, 'r'),
                                'xml', from_encoding='utf-8')
    for elem in soup.findAll('item'):
        if elem.get('android:id') == '@+id/menu_openfile':
            elem['android:showAsAction'] = "never"
            elem['android:visible'] = "false"
    flushxml(soup, 'menu', menu_xml, head=False)


def step_update_xml_nodes(jsdata, **options):
    ''' change package-named item reference in preference UI xml '''

    move_to_android_placeholder()

    # rename settings.SliderPreference node in res/xml/preferences.xml
    preferences_xml = os.path.join(ANDROID_PATH, 'res', 'xml',
                                   'preferences.xml')
    soup = soup = BeautifulSoup(open(preferences_xml, 'r'),
                                'xml', from_encoding='utf-8')
    item = soup.find('org.kiwix.kiwixmobile.settings.SliderPreference')
    item.name = '{}.settings.SliderPreference'.format(jsdata.get('package'))
    flushxml(soup, 'PreferenceScreen', preferences_xml, head=False)

    # rename AnimatedProgressBar node in res/layout/toolbar.xml
    toolbar_xml = os.path.join(ANDROID_PATH, 'res', 'layout', 'toolbar.xml')
    soup = soup = BeautifulSoup(open(toolbar_xml, 'r'),
                                'xml', from_encoding='utf-8')
    item = soup.find('org.kiwix.kiwixmobile.AnimatedProgressBar')
    item.name = '{}.AnimatedProgressBar'.format(jsdata.get('package'))
    flushxml(soup, 'RelativeLayout', toolbar_xml, head=False)


def step_update_gradle(jsdata, **options):
    ''' uncomment compiling the content-libs.jar file into the APK '''

    if not jsdata.get('embed_zim'):
        return

    move_to_android_placeholder()

    # rename settings.SliderPreference node in res/xml/preferences.xml
    fpath = os.path.join(ANDROID_PATH, 'build.gradle')
    lines = open(fpath, 'r').readlines()
    for idx, line in enumerate(lines):
        if 'content-libs.jar' in line:
            lines[idx] = ("    {}\n"
                          .format(re.sub(r'^//', '', line.strip()).strip()))
    with open(fpath, 'w') as f:
        f.write(''.join(lines))


def step_update_android_manifest(jsdata, **options):
    ''' update AndroidManifest.xml to set package, name, version

        and remove intents (so that it's not a ZIM file reader) '''

    move_to_android_placeholder()

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

    # remove useless permissions
    for permission in soup.findAll('uses-permission'):
        if permission['android:name'].replace("android.permission.", "") \
                in USELESS_PERMISSIONS:
            permission.replace_with('')

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


def step_update_kiwix_c(jsdata, **options):
    ''' rewrite imports in JNI/C to match new package '''

    move_to_android_placeholder()

    # rewrite kiwix.c for JNI
    fpath = os.path.join(ANDROID_PATH, 'kiwix.c')
    content = open(fpath, 'r').read()
    with open(fpath, 'w') as f:
        f.write(content.replace('org_kiwix_kiwixmobile',
                                "_".join(jsdata.get('package').split('.'))))


def step_compile_libkiwix(jsdata, **options):
    ''' launch the native libkiwix script without building an APK '''

    move_to_android_placeholder()

    # compile libkiwix and all dependencies
    syscall('./build-android-with-native.py '
            '--toolchain '
            '--lzma '
            '--icu '
            '--zim '
            '--kiwix '
            '--strip '
            '--locales ')


def step_embed_zimfile(jsdata, **options):
    ''' prepare a content-libs.jar file with ZIM file for inclusion in APK '''

    if not jsdata.get('embed_zim'):
        return

    move_to_android_placeholder()

    # create content-libs.jar
    tmpd = tempfile.mkdtemp()
    archs = os.listdir('libs')
    for arch in archs:
        os.makedirs(os.path.join(tmpd, 'lib', arch))
        # shutil.copy(os.path.join('libs', arch, 'libkiwix.so'),
        #             os.path.join(tmpd, 'lib', arch, 'libkiwix.so'))
    copy_to(jsdata.get('zim_file'),
            os.path.join(tmpd, 'lib', archs[0], jsdata.get('zim_name')))
    for arch in archs[1:]:
        os.chdir(os.path.join(tmpd, 'lib', arch))
        os.symlink('../{}/{}'.format(archs[0], jsdata.get('zim_name')),
                   jsdata.get('zim_name'))
    os.chdir(tmpd)
    syscall('zip -r -0 -y {} lib'
            .format(os.path.join(ANDROID_PATH, 'content-libs.jar')))
    shutil.rmtree(tmpd)


def step_build_apk(jsdata, **options):
    ''' build the actual APK '''

    move_to_android_placeholder()

    # compile KiwixAndroid
    syscall('./build-android-with-native.py '
            '--apk '
            '--clean ')


def step_move_apk_to_destination(jsdata, **options):
    ''' place and rename built APKs to main output directory '''

    move_to_current_folder()

    # ensure target directory exists (might not if kiwix was not built)
    try:
        os.makedirs(os.path.join(CURRENT_PATH, 'build', 'outputs', 'apk'))
    except OSError:
        pass
    # move generated APK to satisfy other scripts
    for variant in ('debug', 'debug-unaligned', 'release-unsigned'):
        shutil.move(os.path.join(ANDROID_PATH, 'build', 'outputs', 'apk',
                                 "{}-{}.apk"
                                 .format(jsdata.get('package'), variant)),
                    os.path.join(CURRENT_PATH, 'build', 'outputs', 'apk',
                                 "{}-{}.apk"
                                 .format(jsdata.get('package'), variant)))


def step_remove_android_placeholder(jsdata, **options):
    ''' remove created (temp) android placeholder (useless is success) '''

    move_to_current_folder()

    # delete temp folder
    shutil.rmtree(ANDROID_PATH)


def step_list_output_apk(jsdata, **options):
    ''' ls on the expected APK to check presence and size '''

    move_to_current_folder()

    syscall('ls -lh build/outputs/apk/{}-*'
            .format(jsdata.get('package')), shell=True)


ARGS_MATRIX = OrderedDict([
    ('setup', step_create_android_placeholder),
    ('icons', step_prepare_launcher_icons),
    ('branding', step_update_branding_xml),
    ('constants', step_gen_constants_java),
    ('menu', step_update_main_menu_xml),
    ('xmlnodes', step_update_xml_nodes),
    ('manifest', step_update_android_manifest),
    ('jni', step_update_kiwix_c),
    ('libkiwix', step_compile_libkiwix),
    ('embed', step_embed_zimfile),
    ('gradle', step_update_gradle),
    ('build', step_build_apk),
    ('move', step_move_apk_to_destination),
    ('list', step_list_output_apk),
    ('clean', step_remove_android_placeholder),
])


def usage(arg0, exit=None):
    usage_txt = "Usage: {} <json_file>".format(arg0)
    for idx, step in enumerate(ARGS_MATRIX.keys()):
        if idx > 0 and idx % 3 == 0:
            usage_txt += "\n\t\t\t\t\t\t"
        usage_txt += " [--{}]".format(step)
    print(usage_txt)
    print("\tjson_file:\t\tmandatory parameter holder (cf. source for sample)")
    print("\t--step:\t\t\trun this step. if none specified, all are run.")
    if exit is not None:
        sys.exit(exit)


def main(jspath, **options):

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
    for key in REQUIRED_FIELDS:
        if key not in jsdata.keys():
            logger.error("Required field `{}` is missing from JSON file."
                         .format(key))
            logger.error("Required fields are: {}"
                         .format(", ".join(REQUIRED_FIELDS)))
            sys.exit(1)

    def zim_name_from_path(path):
        fname = path.rsplit('/', 1)[-1]
        return re.sub(r'[^a-z0-9\_.]+', '_', fname.lower())

    # ensure ZIM file is present and find file size
    jsdata.update({'zim_size': str(get_file_size(jsdata.get('zim_file')))})
    jsdata.update({'zim_name': zim_name_from_path(jsdata.get('zim_file'))})
    if jsdata.get('embed_zim'):
        jsdata.update({'zim_name': 'libcontent.so'})

    # greetings
    logger.info("Your are now building {app_name} version {version_name} "
                "at {path} for {zim_name}"
                .format(app_name=jsdata.get('app_name'),
                        version_name=jsdata.get('version_name'),
                        path=ANDROID_PATH,
                        zim_name=jsdata.get('zim_name')))

    # loop through each step and execute if requested by command line
    for step_name, step_func in ARGS_MATRIX.items():
        if options.get('do_{}'.format(step_name), False):
            move_to_android_placeholder()
            step_func(jsdata, **options)
    move_to_current_folder()

if __name__ == '__main__':

    # ensure we were provided a JSON file as first argument
    if len(sys.argv) < 2:
        usage(sys.argv[0], 1)
    else:
        jspath = sys.argv[1]
        args = sys.argv[2:]

    if len(args) == 0:
        options = OrderedDict([('do_{}'.format(step), True)
                               for step in ARGS_MATRIX.keys()])
    else:
        options = OrderedDict()
        for arg in args:
            step_name = re.sub(r'^\-\-', '', arg)
            if step_name not in ARGS_MATRIX.keys():
                logger.error("{} not a valid step. Exiting.".format(step_name))
                usage(sys.argv[0], 1)
            else:
                options.update({'do_{}'.format(step_name): True})

    main(jspath, **options)
