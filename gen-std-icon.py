#!/usr/bin/env python
# -*- coding: utf-8 -*-
# vim: ai ts=4 sts=4 et sw=4 nu

''' Generate an Android ic_launcher friendly icon from a PNG logo

    Generated icon is a 512x512 piels wide 24b transparent PNG.
    It contains a white rounded-square background canvas (which can be
        customized by changing its template in templates/)
    It then adds a resized version of the provided logo in the center
    Then adds two markers:
        An offline marker indicating it's OFFLINE (bared WiFi icon)
        A lang  marker using a bubbled flag

    Script can be called with either a local PNG file or an URL to PNG.

    The supported languages are based on the template flag-bubbles icons. '''

from __future__ import (unicode_literals, absolute_import,
                        division, print_function)
import logging
import sys
import os
import re
import struct
import tempfile
import shutil
import StringIO
from subprocess import call

SIZE = 512  # final icon size
WHITE_CANVAS_SIZE = 464
MARKER_SIZE = 85  # square size of the offline and lang markers
LANG_POSITION = (42, 386)  # X, Y of language code marker
OFFLINE_POSITION = (LANG_POSITION[0] + MARKER_SIZE + SIZE * 0.05,
                    LANG_POSITION[1])
INNER_LOGO_SIZE = 415  # maximum logo square size
CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))
SUPPORTED_LANGS = [re.search(r'launcher\-flag\-([a-z]{2})\.png', fname)
                     .group(1)
                   for fname in os.listdir(os.path.join(CURRENT_PATH,
                                                        'templates'))
                   if 'launcher-flag' in fname]

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

# external dependencies (make sure we're all set up!)
try:
    import requests
except ImportError:
    logger.error("Missing dependency: Unable to import requests.\n"
                 "Please install requests with "
                 "`pip install requests "
                 "either on your machine or in a virtualenv.")
    sys.exit(1)


def get_image_info(data):
    if is_png(data):
        w, h = struct.unpack(b'>LL', data[16:24])
        width = int(w)
        height = int(h)
    else:
        raise Exception('not a png image')
    return width, height


def is_png(data):
    return (data[:8] == b'\211PNG\r\n\032\n'and (data[12:16] == 'IHDR'))


def syscall(args, shell=False, with_print=True):
    ''' execute an external command. Use shell=True if using bash specifics '''
    args = args.split()
    if with_print:
        print("-----------\n" + u" ".join(args) + "\n-----------")

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
    return re.match(r'^https?\:', path)


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


def resize(path, width, height, new_path=None):
    if new_path is None:
        new_path = path
    syscall('convert {inf} -resize {width}x{height} {outf}'
            .format(inf=path, width=width, height=height, outf=new_path))


def sq_resize(path, size, new_path=None):
    return resize(path, size, size, new_path)


def main(logo_path, lang_code):

    if lang_code not in SUPPORTED_LANGS:
        logger.error("No image template for language code `{}`.\n"
                     "Please download a square PNG bubble flag for that lang "
                     "and store it in templates/ with proper name."
                     .format(lang_code))
        sys.exit(1)

    # create a temp directory to store our stalls
    tmpd = tempfile.mkdtemp()

    logger.info("Creating android launcher icon for {}/{} using {}"
                .format(logo_path, lang_code, tmpd))

    # download/copy layer1 (logo)
    layer1 = os.path.join(tmpd, 'layer1.png')
    copy_to(logo_path, layer1)

    if not os.path.exists(layer1) or not os.path.isfile(layer1):
        logger.error("Unable to find logo file at `{}`".format(layer1))
        sys.exit(1)

    try:
        logo_w, logo_h = get_image_info(open(layer1, 'r').read())
    except:
        logger.error("Unable to get logo width and height. Is it a PNG file?")
        sys.exit(1)

    if not logo_w == logo_h:
        logger.warning("Your logo image is not square ({}x{}). "
                       "Result might be ugly..."
                       .format(logo_w, logo_h))
    else:
        logger.debug("PNG file is {}x{}".format(logo_w, logo_h))

    # resize logo so it fits in both image and white canvas
    if logo_w > INNER_LOGO_SIZE or logo_h > INNER_LOGO_SIZE:
        logger.debug("resizing logo to fit in {0}x{0}".format(INNER_LOGO_SIZE))
        sq_resize(layer1, INNER_LOGO_SIZE, layer1)

    # multiply white background and logo
    layer0 = os.path.join(CURRENT_PATH, 'templates',
                          'launcher-background-white.png')
    layer0p1 = os.path.join(tmpd, 'layer0_layer1.png')

    syscall('composite -gravity center {l1} {l0} {l0p1}'
            .format(l1=layer1, l0=layer0, l0p1=layer0p1))

    # prepare layer2 (offline marker)
    offline_mk = os.path.join(CURRENT_PATH, 'templates',
                              'launcher-marker-offline.png')
    layer2 = os.path.join(tmpd, 'layer2.png')
    sq_resize(offline_mk, MARKER_SIZE, layer2)

    # prepare layer3 (lang marker)
    lang_mk = os.path.join(CURRENT_PATH, 'templates',
                           'launcher-flag-{}.png'.format(lang_code))
    layer3 = os.path.join(tmpd, 'layer3.png')
    sq_resize(lang_mk, MARKER_SIZE, layer3)

    # multiply layer0p1 (white + logo) with offline marker (layer2)
    layer1p2 = os.path.join(tmpd, 'layer1_layer2.png')
    syscall('composite -geometry +{x}+{y} {l2} {l0p1} {l1p2}'
            .format(l2=layer2, l0p1=layer0p1, l1p2=layer1p2,
                    x=OFFLINE_POSITION[0], y=OFFLINE_POSITION[1]))

    # multiply layer1p2 (white + logo + offline) with lang marker (layer3)
    layer2p3 = os.path.join(tmpd, 'layer2_layer3.png')
    syscall('composite -geometry +{x}+{y} {l3} {l1p2} {l2p3}'
            .format(l3=layer3, l1p2=layer1p2, l2p3=layer2p3,
                    x=LANG_POSITION[0], y=LANG_POSITION[1]))

    # copy final result to current directory
    icon_path = os.path.join(CURRENT_PATH,
                             'ic_launcher_512_{}.png'.format(lang_code))
    shutil.copy(layer2p3, icon_path)

    # remove temp directory
    shutil.rmtree(tmpd)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Usage:\t{} <logo_path> <lang-code>'.format(sys.argv[0]))
        sys.exit(1)
    main(*sys.argv[1:])
