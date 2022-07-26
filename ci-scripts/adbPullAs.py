#!/usr/bin/env python3
# Copyright (C) 2022
# ViliusSutkus89.com
# https://github.com/ViliusSutkus89/adbPullAs
#
# adbPullAs - adb pull wrapper to pull package private files from Android device
#
# adbPullAs is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

import getopt
import os
import subprocess
import sys
from pathlib import PurePosixPath, PurePath

version = '1.0.0' ####################### buffer for in-place file modification, in case version STRING needs to grow


class FsTest:
    def __init__(self, package_name):
        self.package_name = package_name

    def __implementation(self, test, path):
        # old Androids exit "adb shell false" with exit code 0... work this around
        inner_command = f'"{test} {path}" || echo NOT'
        return not subprocess.run(('adb', 'shell', 'run-as', self.package_name,
                                   'sh', '-c', inner_command),
                                  capture_output=True, text=True).stdout

    def exists(self, path):
        return self.__implementation('test -e', path)

    def is_directory(self, path):
        return self.__implementation('test -d', path)

    def is_file(self, path):
        return self.__implementation('test -f', path)


class AdbPullAs:
    def __init__(self, package_name, local):
        self.fs_test = FsTest(package_name)
        self.tmp_dir = self.__mkdir_unique('/data/local/tmp/adbPullAs')
        self.package_name = package_name
        self.run_as_package = ('adb', 'shell', 'run-as', self.package_name)
        self.local = local

    def __del__(self):
        subprocess.run(('adb', 'shell', 'rmdir', self.tmp_dir))

    @staticmethod
    def __mkdir_unique(suggested_path):
        i = 0
        path = suggested_path
        while len(subprocess.run(('adb', 'shell', 'mkdir', path), capture_output=True).stdout):
            i += 1
            path = f'{suggested_path}-{i}'
        return path

    def __pull_file(self, remote, local_dir):
        filename = remote.name
        device_tmp_file = PurePosixPath(self.tmp_dir, filename)

        # cat-pipe from package private to /data/local/tmp
        subprocess.run(self.run_as_package + ('cat', remote, '>', device_tmp_file), shell=False)

        subprocess.run(('adb', 'pull', device_tmp_file, local_dir))
        subprocess.run(('adb', 'shell', 'rm', device_tmp_file))

    def __pull(self, remote_entry_point, sub_items=()):
        remote_dir = remote_entry_point
        local_dir = self.local
        for s in sub_items:
            remote_dir = PurePosixPath(remote_dir, s)
            local_dir = PurePath(local_dir, s)

        print('Pulling', remote_dir, '->', local_dir)

        if not self.fs_test.exists(remote_dir):
            print(remote_dir, 'does not exist!', file=sys.stderr)
            return False

        elif self.fs_test.is_directory(remote_dir):
            if sub_items:
                try:
                    os.mkdir(local_dir)
                except FileExistsError:
                    pass

            ls_proc = subprocess.run(self.run_as_package + ('ls', f'{remote_dir}/'), capture_output=True, text=True)
            for dir_entry in ls_proc.stdout.splitlines():
                dir_entry_file = PurePosixPath(remote_dir, dir_entry)
                if self.fs_test.is_file(dir_entry_file):
                    self.__pull_file(dir_entry_file, local_dir)
                else:
                    self.__pull(remote_entry_point, sub_items + (dir_entry, ))

        return True

    def pull(self, remote):
        remote = PurePosixPath(remote)
        if self.fs_test.is_file(remote):
            return self.__pull_file(remote, self.local)
        else:
            return self.__pull(remote)


def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:], 'hv', ['help', 'version'])
        for o, a in opts:
            if o in ('-h', '--help'):
                print_usage()
                exit(os.EX_OK)
            elif o in ('-v', '--version'):
                print_version()
                exit(os.EX_OK)
    except getopt.GetoptError as err:
        print(err, "\n", file=sys.stderr)
        print_usage(sys.stderr)
        exit(os.EX_USAGE)

    args = sys.argv[1:]
    if 2 > len(args):
        print_usage(sys.stderr)
        exit(os.EX_USAGE)

    package_name = args.pop(0)

    first_remote = args.pop(0)
    local = args.pop() if len(args) else '.'
    args.insert(0, first_remote)

    if not os.path.isdir(local):
        print(f'"{local}" - COMPUTER_DESTINATION_DIR is not a directory', file=sys.stderr)
        exit(os.EX_NOINPUT)

    apa = AdbPullAs(package_name, local)
    exit_code = os.EX_OK
    for remote in args:
        if not apa.pull(remote):
            exit_code = os.EX_IOERR

    exit(exit_code)


def print_usage(output_to=sys.stdout):
    print(os.path.basename(sys.argv[0]), 'usage:', file=output_to)
    print(os.path.basename(sys.argv[0]), 'PACKAGE_NAME ANDROID_SOURCE... COMPUTER_DESTINATION_DIR\n', file=output_to)
    print('COMPUTER_DESTINATION_DIR can be omitted to pull into current working directory,', file=output_to)
    print('\tbut only with a single supplied ANDROID_SOURCE (first example).', file=output_to)
    print('Multiple ANDROID_SOURCEs require COMPUTER_DESTINATION_DIR to be supplied.\n', file=output_to)
    print('Examples:', file=output_to)
    pn = 'com.viliussutkus89.application'
    print(sys.argv[0], pn, f'/data/data/{pn}/databases/androidx.work.workdb', file=output_to)
    print(sys.argv[0], pn, f'/data/data/{pn}/cache', f'/data/data/{pn}/files',
          './pulled_from_device', file=output_to)


def print_version():
    print(os.path.basename(sys.argv[0]), '-', 'adb pull wrapper to pull package private files from Android device')
    print('version: ', version)
    print()
    print('THIS WORKS ONLY ON DEBUG APPLICATIONS')
    print()
    print('Copyright (C) 2022')
    print('ViliusSutkus89.com')
    print('https://github.com/ViliusSutkus89/adbPullAs')
    print()
    print('adbPullAs is free software: you can redistribute it and/or modify')
    print('it under the terms of the GNU General Public License version 3,')
    print('as published by the Free Software Foundation.')
    print()
    print('This program is distributed in the hope that it will be useful,')
    print('but WITHOUT ANY WARRANTY; without even the implied warranty of')
    print('MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the')
    print('GNU General Public License for more details.')
    print()
    print('You should have received a copy of the GNU General Public License')
    print('along with this program.  If not, see <https://www.gnu.org/licenses/>.')


if __name__ == '__main__':
    main()
