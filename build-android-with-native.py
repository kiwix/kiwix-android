#!/usr/bin/env python

''' Compiles Kiwix dependencies for Android

    . Compile liblzma
    . Compile libzim
    . Compile libkiwix '''

import os
import re
import sys
import copy
import shutil
from xml.dom.minidom import parse
from subprocess import call, check_output

# target platform to compile for
# list of available toolchains in <NDK_PATH>/toolchains
# arm-linux-androideabi, mipsel-linux-android, x86, llvm
ALL_ARCHS = ['arm-linux-androideabi', 'mipsel-linux-android', 'x86']


def find_package():
    d = parse('AndroidManifest.xml')
    return [e.getAttribute('package').strip()
            for e in d.getElementsByTagName('manifest')][-1]

PACKAGE = find_package()

USAGE = '''Usage:  {arg0} [--option]

    Without option, all steps are executed on all archs.

    --toolchain     Creates the toolchain
    --lzma          Compile liblzma
    --icu           Compile libicu
    --zim           Compile libzim
    --kiwix         Compile libkiwix
    --strip         Strip libkiwix.so
    --locales       Create the locales.txt file
    --apk           Create an APK file
    --clean         Remove build folder (except apk files)

    Note that the '--' prefix is optional.

    --on=ARCH       Disable steps on all archs and cherry pick the ones wanted.
                    Multiple --on=ARCH can be specified.
                    ARCH in 'armeabi', 'mips', 'x86'. '''


def init_with_args(args):

    def display_usage():
        print(USAGE.format(arg0=args[0]))
        sys.exit(0)

    # default is executing all the steps
    create_toolchain = compile_liblzma = compile_libicu = \
        compile_libzim = compile_libkiwix = strip_libkiwix = \
        compile_apk = locales_txt = clean = True
    archs = ALL_ARCHS

    options = [a.lower() for a in args[1:]]

    # print usage if help is requested
    for help_str in ('-h', '--help'):
        if options.count(help_str):
            display_usage()

    # do we have an --on= flag?
    if '--on=' in u' '.join(args):
        # yes, so we clear the arch list and build from request
        archs = []
        # store options on a dict so we can safely remove as we process
        doptions = {}
        for idx, param in enumerate(options):
            doptions[idx] = param
        # add found arch to list of archs
        for idx, param in doptions.items():
            if param.startswith('--on='):
                try:
                    rarch = param.split('=', 1)[1]
                    archs.append([k for k, v in ARCHS_SHORT_NAMES.items()
                                  if rarch == v][0])
                except:
                    pass
                doptions.pop(idx)
        # recreate options list from other items
        options = [v for v in doptions.values() if not v.startswith('--on=')]

    if len(options):
        # we received options.
        # consider we only want the specified steps
        create_toolchain = compile_liblzma = compile_libicu = compile_libzim = \
            compile_libkiwix = strip_libkiwix = \
            compile_apk = locales_txt = clean = False

        for option in options:
            if 'toolchain' in option:
                create_toolchain = True
            if 'lzma' in option:
                compile_liblzma = True
            if 'icu' in option:
                compile_libicu = True
            if 'zim' in option:
                compile_libzim = True
            if 'kiwix' in option:
                compile_libkiwix = True
            if 'strip' in option:
                strip_libkiwix = True
            if 'apk' in option:
                compile_apk = True
            if 'locales' in option:
                locales_txt = True
            if 'clean' in option:
                clean = True

    return (create_toolchain, compile_liblzma, compile_libicu, compile_libzim,
            compile_libkiwix, strip_libkiwix, compile_apk, locales_txt,
            clean, archs)

# store the OS's environment PATH as we'll mess with it
# ORIGINAL_ENVIRON_PATH = os.environ.get('PATH')
ORIGINAL_ENVIRON = copy.deepcopy(os.environ)

# the directory of this file for relative referencing
CURRENT_PATH = os.path.dirname(os.path.abspath(__file__))

# the parent directory of this file for relative referencing
PARENT_PATH = os.path.dirname(CURRENT_PATH)

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

# find out what to execute based on command line arguments
CREATE_TOOLCHAIN, COMPILE_LIBLZMA, COMPILE_LIBICU, COMPILE_LIBZIM, \
    COMPILE_LIBKIWIX, STRIP_LIBKIWIX, COMPILE_APK, \
    LOCALES_TXT, CLEAN, ARCHS = init_with_args(sys.argv)

# compiler version to use
# list of available toolchains in <NDK_PATH>/toolchains
# 4.4.3, 4.6, 4.7, clang3.1, clang3.2
COMPILER_VERSION = '4.8'

# location of Android NDK
NDK_PATH = os.environ.get('NDK_PATH',
                          os.path.join(os.path.dirname(CURRENT_PATH),
                                       'src', 'dependencies',
                                       'android-ndk-r10e'))
SDK_PATH = os.environ.get('ANDROID_HOME',
                          os.path.join(os.path.dirname(CURRENT_PATH),
                                       'src', 'dependencies',
                                       'android-sdk'))

# Target Android EABI/version to compile for.
# list of available platforms in <NDK_PATH>/platforms
# android-14, android-3, android-4, android-5, android-8, android-9
NDK_PLATFORM = os.environ.get('NDK_PLATFORM', 'android-14')

# will contain the different prepared toolchains for a specific build
PLATFORM_PREFIX = os.environ.get('PLATFORM_PREFIX',
                                 os.path.join(PARENT_PATH, 'platforms'))
if not os.path.exists(PLATFORM_PREFIX):
    os.makedirs(PLATFORM_PREFIX)

# root folder for liblzma
LIBLZMA_SRC = os.path.join(os.path.dirname(CURRENT_PATH),
                           'src', 'dependencies', 'xz')

# headers for liblzma
LIBLZMA_INCLUDES = [os.path.join(LIBLZMA_SRC, 'src', 'liblzma', 'api')]

# root folder for libicu
LIBICU_SRC = os.path.join(os.path.dirname(CURRENT_PATH),
                          'src', 'dependencies', 'icu', 'source')

# headers for libicu
LIBICU_INCLUDES = [os.path.join(LIBICU_SRC, 'i18n'),
                   os.path.join(LIBICU_SRC, 'common')]

# root folder for libzim
LIBZIM_SRC = os.path.join(os.path.dirname(CURRENT_PATH),
                          'src', 'dependencies', 'zimlib-1.2')

# headers for libzim
LIBZIM_INCLUDES = [os.path.join(LIBZIM_SRC, 'include')]

# source files for building libzim
LIBZIM_SOURCE_FILES = ('article.cpp', 'articlesearch.cpp', 'cluster.cpp',
                       'dirent.cpp', 'file.cpp', 'fileheader.cpp',
                       'fileimpl.cpp', 'indexarticle.cpp', 'ptrstream.cpp',
                       'search.cpp', 'template.cpp', 'unicode.cpp', 'uuid.cpp',
                       'zintstream.cpp', 'envvalue.cpp', 'lzmastream.cpp',
                       'unlzmastream.cpp', 'fstream.cpp', 'md5.c',
                       'md5stream.cpp')

# root folder for libkiwix
LIBKIWIX_SRC = os.path.join(os.path.dirname(CURRENT_PATH),
                            'src', 'common')

OPTIMIZATION_ENV = {'CXXFLAGS': ' -D__OPTIMIZE__ -fno-strict-aliasing '
                                ' -DU_HAVE_NL_LANGINFO_CODESET=0 '
                                '-DU_STATIC_IMPLEMENTATION '
                                '-DU_HAVE_STD_STRING -DU_TIMEZONE=0',
                    'NDK_DEBUG': '0'}

# list of path that should already be set
REQUIRED_PATHS = (NDK_PATH, PLATFORM_PREFIX,
                  LIBLZMA_SRC, LIBZIM_SRC, LIBKIWIX_SRC)

# list of paths for libicu
ICU_TMP = PLATFORM_PREFIX + '/tmp/'
ICU_TMP_HOST = ICU_TMP + 'host/'
ICU_TMP_TARGET = ICU_TMP + 'target/'


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


def failed_on_step(error_msg):
    print('[ERROR] %s. Aborting.' % error_msg)
    sys.exit(1)

# check that required paths are in place before we start
for path in REQUIRED_PATHS:
    fail_on_missing(path)

# store where we are so we can go back
    curdir = os.getcwd()

# Prepare the libicu cross-compilation
if COMPILE_LIBICU:
    if (not os.path.exists(ICU_TMP)):
        os.mkdir(ICU_TMP)
    if (not os.path.exists(ICU_TMP_HOST)):
        os.mkdir(ICU_TMP_HOST)
    if (not os.path.exists(ICU_TMP_TARGET)):
        os.mkdir(ICU_TMP_TARGET)
    os.chdir(ICU_TMP_HOST)
    syscall(LIBICU_SRC + '/configure --with-data-packaging=archive',
            shell=True)
    syscall('make', shell=True)
    os.chdir(os.getcwd())

for arch in ARCHS:
    # second name of the platform ; used as subfolder in platform/
    arch_full = ARCHS_FULL_NAMES.get(arch)
    arch_short = ARCHS_SHORT_NAMES.get(arch)

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
        syscall('ln -sf %(src)s %(dest)s/'
                % {'src': ln_src, 'dest': dest})

    # check that the step went well
    if CREATE_TOOLCHAIN or COMPILE_LIBLZMA or COMPILE_LIBZIM or \
       COMPILE_LIBKIWIX or STRIP_LIBKIWIX:
        if (not os.path.exists(os.path.join(platform, arch_full, 'bin', 'gcc'))
            or not os.path.exists(os.path.join(platform,
                                               arch_full, 'libexec'))):
            failed_on_step('The toolchain was not '
                           'copied properly and is not present.')

    # change the PATH for compilation to use proper tools
    new_environ = {'PATH': ('%(platform)s/bin:%(platform)s/%(arch_full)s'
                            '/bin:%(platform)s/libexec/gcc/%(arch_full)s/'
                            '%(gccver)s/:%(sdka)s:%(sdkb)s/:%(orig)s'
                   % {'platform': platform,
                      'orig': ORIGINAL_ENVIRON['PATH'],
                      'arch_full': arch_full,
                      'gccver': COMPILER_VERSION,
                      'sdka': os.path.join(SDK_PATH, 'platform-tools'),
                      'sdkb': os.path.join(SDK_PATH, 'tools')}),
                   'CFLAGS': ' -fPIC -D_FILE_OFFSET_BITS=64 ',
                   'ANDROID_HOME': SDK_PATH}
    change_env(new_environ)
    change_env(OPTIMIZATION_ENV)

    # check that the path has been changed
    if platform not in os.environ.get('PATH'):
        failed_on_step('The PATH environment variable was not set properly.')

    # compile liblzma.a, liblzma.so
    os.chdir(LIBLZMA_SRC)
    configure_cmd = ('./configure --host=%(arch)s --prefix=%(platform)s '
                     '--disable-assembler --enable-shared --enable-static '
                     '--enable-largefile'
                     % {'arch': arch_full,
                        'platform': platform})
    if COMPILE_LIBLZMA:
        # configure, compile, copy and clean liblzma from official sources.
        # even though we need only static, we conpile also shared so it
        # switches the -fPIC properly.
        syscall(configure_cmd, shell=True)
        syscall('make clean', shell=True)
        syscall('make', shell=True)
        syscall('make install', shell=True)
        syscall('make clean', shell=True)

    # check that the step went well
    if COMPILE_LIBLZMA or COMPILE_LIBZIM or COMPILE_LIBKIWIX:
        if not os.path.exists(os.path.join(platform, 'lib', 'liblzma.a')):
            failed_on_step('The liblzma.a archive file has not been created '
                           'and is not present.')

    # compile libicu.a, libicu.so
    os.chdir(ICU_TMP_TARGET)
    configure_cmd = (LIBICU_SRC + '/configure --host=%(arch)s --enable-static '
                     '--prefix=%(platform)s --with-cross-build=%(icu)s  '
                     '--disable-shared --with-data-packaging=archive '
                     % {'arch': arch_full, 'platform': platform,
                        'icu': ICU_TMP_HOST})

    if COMPILE_LIBICU:
        # configure, compile, copy and clean libicu from official sources.
        # even though we need only static, we conpile also shared so it
        # switches the -fPIC properly.
        syscall(configure_cmd, shell=True)
        syscall('make clean', shell=True)
        syscall('make VERBOSE=1', shell=True)
        syscall('make install', shell=True)
        syscall('make clean', shell=True)

    # check that the step went well
    if COMPILE_LIBICU or COMPILE_LIBKIWIX:
        if not os.path.exists(os.path.join(platform, 'lib', 'libicui18n.a')):
            failed_on_step("The libicu.a archive file "
                           "has not been created for {} and is not present."
                           .format(platform))

    # create libzim.a
    os.chdir(curdir)
    platform_includes = ['%(platform)s/include/c++/%(gccver)s/'
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
                         ]

    src_dir = os.path.join(LIBZIM_SRC, 'src')
    compile_cmd = ('g++ -fPIC -c -D_FILE_OFFSET_BITS=64 -DHAVE_LSEEK64 '
                   '-D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE '
                   '-B%(platform)s/sysroot '
                   '%(source_files)s -I%(include_paths)s '
                   % {'platform': platform,
                      'arch_full': arch_full,
                      'gccver': COMPILER_VERSION,
                      'source_files': ' '.join([os.path.join(src_dir, src)
                                                for src
                                                in LIBZIM_SOURCE_FILES]),
                      'include_paths': ' -I'.join(LIBLZMA_INCLUDES
                                                  + LIBICU_INCLUDES
                                                  + LIBZIM_INCLUDES
                                                  + platform_includes)})
    link_cmd = ('ar rvs libzim.a '
                '%(obj_files)s '
                % {'obj_files': ' '.join([re.sub('(\.c[p]*)', '.o', n)
                                          for n in LIBZIM_SOURCE_FILES])})

    if COMPILE_LIBZIM:
        syscall(compile_cmd)
        syscall(link_cmd)

        libzim_file = os.path.join(curdir, 'libzim.a')
        shutil.copy(libzim_file, os.path.join(platform, 'lib'))
        os.remove(libzim_file)

        for src in LIBZIM_SOURCE_FILES:
            os.remove(re.sub('(\.c[p]*)', '.o', src))

    # check that the step went well
    if COMPILE_LIBZIM or COMPILE_LIBKIWIX:
        if not os.path.exists(os.path.join(platform, 'lib', 'libzim.a')):
            failed_on_step('The libzim.a archive file has not been created '
                           'and is not present.')

    # create libkiwix.so
    os.chdir(curdir)
    compile_cmd = ('g++ -std=c++11 -std=gnu++11 -fPIC -c -B%(platform)s/sysroot '
                   '-DU_HAVE_STD_STRING '
                   '-D_FILE_OFFSET_BITS=64 '
                   '-D_LARGEFILE_SOURCE -D_LARGEFILE64_SOURCE '
                   '-DANDROID_NDK '
                   'kiwix.c %(kwsrc)s/kiwix/reader.cpp '
                   '%(kwsrc)s/stringTools.cpp '
                   '%(kwsrc)s/pathTools.cpp '
                   '-I%(include_paths)s '
                   % {'platform': platform,
                      'arch_full': arch_full,
                      'gccver': COMPILER_VERSION,
                      'kwsrc': LIBKIWIX_SRC,
                      'include_paths': ' -I'.join(LIBLZMA_INCLUDES
                                                  + LIBICU_INCLUDES
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
                'kiwix.o reader.o stringTools.o pathTools.o '
                '%(platform)s/lib/gcc/%(arch_full)s/%(gccver)s/crtbegin.o '
                '%(platform)s/lib/gcc/%(arch_full)s/%(gccver)s/crtend.o '
                '%(platform)s/lib/libzim.a %(platform)s/lib/liblzma.a '
                # '%(platform)s/lib/libicutu.a '
                # '%(platform)s/lib/libicuio.a '
                '%(platform)s/lib/libicuuc.a '
                # '%(platform)s/lib/libicule.a '
                # '%(platform)s/lib/libiculx.a '
                # '%(platform)s/lib/libicui18n.a '
                '%(platform)s/lib/libicudata.a '
                '-L%(platform)s/%(arch_full)s/lib '
                '%(NDK_PATH)s/sources/cxx-stl/gnu-libstdc++/%(gccver)s'
                '/libs/%(arch_short)s/libgnustl_static.a '
                '-llog -landroid -lstdc++ -lc -lm -ldl '
                '%(platform)s/lib/gcc/%(arch_full)s/%(gccver)s/libgcc.a '
                '-o %(curdir)s/libs/%(arch_short)s/libkiwix.so'
                % {'kwsrc': LIBKIWIX_SRC,
                   'platform': platform,
                   'arch_full': arch_full,
                   'arch_short': arch_short,
                   'curdir': curdir,
                   'gccver': COMPILER_VERSION,
                   'NDK_PATH': NDK_PATH})

    if COMPILE_LIBKIWIX:

        # compile JNI header
        os.chdir(os.path.join(curdir, 'src', *PACKAGE.split('.')))
        syscall('javac JNIKiwix.java')
        os.chdir(os.path.join(curdir, 'src'))
        syscall('javah -jni {package}.JNIKiwix'.format(package=PACKAGE))
        os.chdir(curdir)

        syscall(compile_cmd)
        syscall(link_cmd)

        for obj in ('kiwix.o', 'reader.o', 'stringTools.o', 'pathTools.o',
                    'src/{}_JNIKiwix.h'.format("_".join(PACKAGE.split('.')))):
            os.remove(obj)

    # check that the step went well
    if COMPILE_LIBKIWIX or STRIP_LIBKIWIX or COMPILE_APK:
        if not os.path.exists(os.path.join('libs', arch_short, 'libkiwix.so')):
            failed_on_step('The libkiwix.so shared lib has not been created '
                           'and is not present.')

    if STRIP_LIBKIWIX:
        syscall('%(platform)s/%(arch_full)s/bin/strip '
                '%(curdir)s/libs/%(arch_short)s/libkiwix.so'
                % {'platform': platform,
                   'arch_full': arch_full,
                   'arch_short': arch_short,
                   'curdir': curdir})

    os.chdir(curdir)
    change_env(ORIGINAL_ENVIRON)

if LOCALES_TXT:

    os.chdir(curdir)

    # Get the path of the res folder
    res_path = os.path.join(curdir, 'res')

    # Get all the ISO 639-1 language codes from the suffix of the value folders
    files = [f.split('values-')[1]
             for f in os.listdir(res_path) if f.startswith('values-')]

    # Append the English Locale to the list,
    # since the default values folder, (the english) values folder
    # does not have a suffix and gets ignored when creating the above list
    files.append('en')

    # Create a CSV file with all the langauge codes in the assets folder
    with open(os.path.join(curdir, 'assets', 'locales.txt'), 'w') as f:
        f.write(",\n".join(files))

    print("Created locales.txt file.")

if COMPILE_APK:

    os.chdir(curdir)

    # rewrite local.properties to target proper SDK
    with open('local.properties', 'w') as f:
        f.write('# {}'.format(check_output('date').strip()))
        f.write('sdk.dir={}'.format(os.path.abspath(SDK_PATH)))

    # store ANDROID_HOME if exist
    android_home = os.environ.get('ANDROID_HOME')

    # update env for that compile
    if android_home:
        change_env({'ANDROID_HOME': os.path.abspath(SDK_PATH)})

    # Compile java and build APK
    syscall('rm -f build/outputs/apk/*.apk', shell=True)
    syscall('./gradlew clean assemble')
    syscall('./gradlew build --stacktrace')

    # compile complete, restore ANDROID_HOME
    if android_home:
        change_env({'ANDROID_HOME': android_home})

    folder_name = os.path.split(curdir)[-1]

    # Check that the step went well
    if not os.path.exists(
            os.path.join('build', 'outputs', 'apk',
                         '{}-debug-unaligned.apk'.format(folder_name))):
        failed_on_step("The {}-debug-unaligned.apk package "
                       "has not been created and is not present."
                       .format(folder_name))

    # rename APKs for better listing
    for variant in ('debug', 'debug-unaligned', 'release-unsigned'):
        shutil.move(os.path.join('build', 'outputs', 'apk',
                                 "{}-{}.apk".format(folder_name, variant)),
                    os.path.join('build', 'outputs', 'apk',
                                 "{}-{}.apk".format(PACKAGE, variant)))

if CLEAN:

    os.chdir(curdir)
    # remove everything from build folder expect the APKs
    syscall('rm -rf build/generated build/intermediates build/native-libs '
            'build/reports build/test-results build/tmp build/outputs/logs '
            'build/outputs/lint*', shell=True)

# display built APKs
syscall('ls -lh build/outputs/apk/{}-*'.format(PACKAGE), shell=True)
