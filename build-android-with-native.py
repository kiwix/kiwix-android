#!/usr/bin/env python

''' Compiles Kiwix dependencies for Android

    . Compile liblzma
    . Compile libzim
    . Compile libkiwix '''

import os
import sys
import copy
import shutil
from subprocess import call, check_output

# switchs for debugging purposes ; please ignore.
CREATE_TOOLCHAIN = True
COMPILE_LIBLZMA = True
COMPILE_LIBZIM = True
COMPILE_LIBKIWIX = True
STRIP_LIBKIWIX = True
COMPILE_APK = True

# store the OS's environment PATH as we'll mess with it
# ORIGINAL_ENVIRON_PATH = os.environ.get('PATH')
ORIGINAL_ENVIRON = copy.deepcopy(os.environ)

# the directory of this file for relative referencing
CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))

# target platform to compile for
# list of available toolchains in <NDK_PATH>/toolchains
# arm-linux-androideabi, mipsel-linux-android, x86, llvm
ARCHS = ('arm-linux-androideabi', 'mipsel-linux-android', 'x86')

# different names of folder path for accessing files
ARCHS_FULL_NAMES = {
    'arm-linux-androideabi': 'arm-linux-androideabi',
    'mipsel-linux-android': 'mipsel-linux-android',
    'x86': 'i686-linux-android'}
ARCHS_SHORT_NAMES = {
    'arm-linux-androideabi': 'armeabi',
    'mipsel-linux-android': 'mips',
    'x86': 'x86'}

# store host machine name
UNAME = check_output(['uname', '-s']).strip()
UARCH = check_output(['uname', '-m']).strip()
SYSTEMS = {'Linux': 'linux', 'Darwin': 'mac'}

# compiler version to use
# list of available toolchains in <NDK_PATH>/toolchains
# 4.4.3, 4.6, 4.7, clang3.1, clang3.2
COMPILER_VERSION = '4.6'  # /!\ doesn't work with 4.7

# location of Android NDK
NDK_PATH = os.environ.get('NDK_PATH',
                          os.path.join(os.path.dirname(CURRENT_PATH),
                                       'src', 'dependencies',
                                       'android-ndk-r8e'))
SDK_PATH = os.environ.get('ANDROID_HOME',
                          os.path.join(os.path.dirname(CURRENT_PATH),
                                       'src', 'dependencies',
                                       'android-sdk', 'sdk'))

# Target Android EABI/version to compile for.
# list of available platforms in <NDK_PATH>/platforms
# android-14, android-3, android-4, android-5, android-8, android-9
NDK_PLATFORM = os.environ.get('NDK_PLATFORM', 'android-14')

# will contain the different prepared toolchains for a specific build
PLATFORM_PREFIX = os.environ.get('PLATFORM_PREFIX',
                                 os.path.join(CURRENT_PATH, 'platforms'))
if not os.path.exists(PLATFORM_PREFIX):
    os.makedirs(PLATFORM_PREFIX)

# root folder for liblzma
LIBLZMA_SRC = os.path.join(os.path.dirname(CURRENT_PATH),
                           'src', 'dependencies', 'xz')

# headers for liblzma
LIBLZMA_INCLUDES = [os.path.join(LIBLZMA_SRC, 'src', 'liblzma', 'api')]

# root folder for libzim
LIBZIM_SRC = os.path.join(os.path.dirname(CURRENT_PATH),
                          'src', 'zimlib')

# headers for libzim
LIBZIM_INCLUDES = [os.path.join(LIBZIM_SRC, 'include')]

# source files for building libzim
LIBZIM_SOURCE_FILES = ('article.cpp', 'articlesearch.cpp', 'cluster.cpp',
                       'dirent.cpp', 'file.cpp', 'fileheader.cpp',
                       'fileimpl.cpp', 'indexarticle.cpp', 'ptrstream.cpp',
                       'search.cpp', 'template.cpp', 'unicode.cpp', 'uuid.cpp',
                       'zintstream.cpp', 'envvalue.cpp', 'lzmastream.cpp',
                       'unlzmastream.cpp', 'fstream.cpp', 'md5.cpp',
                       'md5stream.cpp')

# root folder for libkiwix
LIBKIWIX_SRC = os.path.join(os.path.dirname(CURRENT_PATH),
                            'src', 'common')

OPTIMIZATION_ENV = {'CXXFLAGS': ' -D__OPTIMIZE__ -fno-strict-aliasing '
                                '-mfpu=vfp -mfloat-abi=softfp ',
                    'NDK_DEBUG': '0'}

# list of path that should already be set
REQUIRED_PATHS = (NDK_PATH, PLATFORM_PREFIX,
                  LIBLZMA_SRC, LIBZIM_SRC, LIBKIWIX_SRC)

def fail_on_missing(path):
    ''' check existence of path and error msg + exit if it fails '''
    if not os.path.exists(path):
        print(u"Required PATH is missing or misdefined: %s.\n"
              u"Check that you have installed the Android NDK properly "
              u"and run 'make' in 'src/dependencies'" % path)
        sys.exit(1)

def syscall(args, shell=False, with_print=True):
    ''' make a system call '''
    args = args.split()
    if with_print:
        print(u"-----------\n" + u" ".join(args) + u"\n-----------")

    if shell:
        args = ' '.join(args)
    call(args, shell=shell)

def change_env(values):
    ''' update a set of environment variables '''
    for k, v in values.items():
        os.environ[k] = v
        syscall('export %s="%s"' % (k, v), shell=True, with_print=False)

# check that required paths are in place before we start
for path in REQUIRED_PATHS:
    fail_on_missing(path)

for arch in ARCHS:
    # second name of the platform ; used as subfolder in platform/
    arch_full = ARCHS_FULL_NAMES.get(arch)
    arch_short = ARCHS_SHORT_NAMES.get(arch)

    # store where we are so we can go back

    curdir = os.getcwd()

    # platform contains the toolchain
    platform = os.path.join(PLATFORM_PREFIX, arch)

    # prepare the toolchain
    toolchain = '%(arch)s-%(version)s' % {'arch': arch,
                                          'version': COMPILER_VERSION}
    toolchain_cmd = ('%(NDK_PATH)s/build/tools/make-standalone-toolchain.sh '
                     '--toolchain=%(toolchain)s '
                     '--platform=%(NDK_PLATFORM)s '
                     '--install-dir=%(PLATFORM_PREFIX)s'
                     % {'NDK_PATH': NDK_PATH,
                        'NDK_PLATFORM': NDK_PLATFORM,
                        'toolchain': toolchain,
                        'PLATFORM_PREFIX': platform})
    # required for compilation on an OSX host
    if UNAME == 'Darwin':
        toolchain_cmd += ' --system=darwin-x86_64'
    elif UNAME == 'Linux':
        if UARCH == 'i686':
            toolchain_cmd += ' --system=linux-x86'
        else:
            toolchain_cmd += ' --system=linux-x86_64'

    if CREATE_TOOLCHAIN:
        # copies the precompiled toolchain for the platform:
        # includes gcc, headers and tools.
        syscall(toolchain_cmd, shell=True)

        # add a symlink for liblto_plugin.so to work
        # could not find how to direct gcc to the right folder
        ln_src = '%(platform)s/libexec' % {'platform': platform}
        dest = '%(platform)s/%(arch_full)s' % {'platform': platform,
                                               'arch_full': arch_full}
        syscall('ln -s %(src)s %(dest)s/'
                % {'src': ln_src, 'dest': dest})

    # change the PATH for compilation to use proper tools
    new_environ = {'PATH': ('%(platform)s/bin:%(platform)s/%(arch_full)s'
                            '/bin:%(platform)s/libexec/gcc/%(arch_full)s/'
                            '%(gccver)s/:%(sdka)s:%(sdkb)s/%(orig)s'
                          % {'platform': platform,
                             'orig': ORIGINAL_ENVIRON['PATH'],
                             'arch_full': arch_full,
                             'gccver': COMPILER_VERSION,
                             'sdka': os.path.join(SDK_PATH,
                                                     'platform-tools'),
                             'sdkb': os.path.join(SDK_PATH, 'tools')}),
                   'CFLAGS': ' -fPIC ',
                   'ANDROID_HOME': SDK_PATH}
    change_env(new_environ)
    change_env(OPTIMIZATION_ENV)

    # compile liblzma.a, liblzma.so
    os.chdir(LIBLZMA_SRC)
    configure_cmd = ('./configure --host=%(arch)s --prefix=%(platform)s '
                     '--disable-assembler --enable-shared --enable-static'
                     % {'arch': arch_full,
                        'platform': platform})
    if COMPILE_LIBLZMA:
        # configure, compile, copy and clean liblzma from official sources.
        # even though we need only static, we conpile also shared so it
        # switches the -fPIC properly.
        syscall(configure_cmd, shell=True)
        syscall('make clean')
        syscall('make')
        syscall('make install')
        syscall('echo $PATH', shell=True)
        syscall('make clean')

    # create libzim.a
    os.chdir(curdir)
    platform_includes = [
                        '%(platform)s/include/c++/%(gccver)s/'
                         % {'platform': platform, 'gccver': COMPILER_VERSION},

                         '%(platform)s/include/c++/%(gccver)s/%(arch_full)s'
                         % {'platform': platform, 'gccver': COMPILER_VERSION,
                            'arch_full': arch_full},

                         '%(platform)s/sysroot/usr/include/'
                         % {'platform': platform},

                         '%(platform)s/lib/gcc/%(arch_full)s/'
                         '%(gccver)s/include'
                         % {'platform': platform, 'arch_full': arch_full,
                            'gccver': COMPILER_VERSION},

                         '%(platform)s/lib/gcc/%(arch_full)s/%(gccver)s'
                         '/include-fixed'
                         % {'platform': platform, 'arch_full': arch_full,
                            'gccver': COMPILER_VERSION},

                         '%(platform)s/sysroot/usr/include/linux/'
                         % {'platform': platform}
                         ]

    src_dir = os.path.join(LIBZIM_SRC, 'src')
    compile_cmd = ('g++ -fPIC -c -D_FILE_OFFSET_BITS=64 '
                   '-D_LARGEFILE64_SOURCE '
                   '-B%(platform)s/sysroot '
                   '%(source_files)s -I%(include_paths)s '
                   % {'platform': platform,
                      'arch_full': arch_full,
                      'gccver': COMPILER_VERSION,
                      'source_files': ' '.join([os.path.join(src_dir, src)
                                                for src
                                                in LIBZIM_SOURCE_FILES]),
                      'include_paths': ' -I'.join(LIBLZMA_INCLUDES
                                                  + LIBZIM_INCLUDES
                                                  + platform_includes)})
    link_cmd = ('ar rvs libzim.a '
                '%(obj_files)s '
                % {'obj_files': ' '.join([n.replace('.cpp', '.o')
                                          for n in LIBZIM_SOURCE_FILES])})

    if COMPILE_LIBZIM:
        syscall(compile_cmd)
        syscall(link_cmd)

        libzim_file = os.path.join(curdir, 'libzim.a')
        shutil.copy(libzim_file, os.path.join(platform, 'lib'))
        os.remove(libzim_file)

        for src in LIBZIM_SOURCE_FILES:
            os.remove(src.replace('.cpp', '.o'))

    # compile JNI header
    os.chdir(os.path.join(curdir, 'src', 'org', 'kiwix', 'kiwixmobile'))
    syscall('javac JNIKiwix.java')
    os.chdir(os.path.join(curdir, 'src'))
    syscall('javah -jni org.kiwix.kiwixmobile.JNIKiwix')

    # create libkiwix.so
    os.chdir(curdir)
    compile_cmd = ('g++ -fPIC -c -B%(platform)s/sysroot '
                   'kiwix.c %(kwsrc)s/kiwix/reader.cpp %(kwsrc)s'
                   '/stringTools.cpp '
                   '-I%(include_paths)s '
                   % {'platform': platform,
                      'arch_full': arch_full,
                      'gccver': COMPILER_VERSION,
                      'kwsrc': LIBKIWIX_SRC,
                      'include_paths': ' -I'.join(LIBLZMA_INCLUDES
                                                  + LIBZIM_INCLUDES
                                                  + platform_includes
                                                  + [LIBKIWIX_SRC,
                                                     os.path.join(LIBZIM_SRC,
                                                                  'include'),
                                                     os.path.join(curdir,
                                                                  'src')])
                      })

    link_cmd = ('g++ -fPIC -shared -B%(platform)s/sysroot '
                '--sysroot %(platform)s/sysroot '
                '-nostdlib '
                'kiwix.o reader.o stringTools.o '
                '%(platform)s/lib/libzim.a %(platform)s/lib/liblzma.a '
                '-L%(platform)s/%(arch_full)s/lib '
                '%(NDK_PATH)s/sources/cxx-stl/gnu-libstdc++/%(gccver)s'
                '/libs/%(arch_short)s/libgnustl_static.a '
                '-llog -landroid -lstdc++ -lc '
                '%(platform)s/lib/gcc/%(arch_full)s/%(gccver)s/libgcc.a '
                '-o %(curdir)s/libs/%(arch_short)s/libkiwix.so'
                % {'kwsrc': LIBKIWIX_SRC,
                   'platform': platform,
                   'arch_full': arch_full,
                   'arch_short': arch_short,
                   'curdir': curdir,
                   'gccver': COMPILER_VERSION,
                   'NDK_PATH': NDK_PATH,
                   'arch_short': arch_short})

    if COMPILE_LIBKIWIX:
        syscall(compile_cmd)
        syscall(link_cmd)

        for obj in ('kiwix.o', 'reader.o', 'stringTools.o',
                    'src/org_kiwix_kiwixmobile_JNIKiwix.h'):
            os.remove(obj)

    if STRIP_LIBKIWIX:
        syscall('%(platform)s/%(arch_full)s/bin/strip '
                '%(curdir)s/libs/%(arch_short)s/libkiwix.so'
                % {'platform': platform,
                   'arch_full': arch_full,
                   'arch_short': arch_short,
                   'curdir': curdir})

    os.chdir(curdir)
    change_env(ORIGINAL_ENVIRON)

if COMPILE_APK:
    syscall('ant debug')
    syscall('ls -lh bin/*.apk', shell=True)
