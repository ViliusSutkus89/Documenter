# Copyright (C) 2022
# ViliusSutkus89.com
# https://github.com/ViliusSutkus89/Documenter
#
# Documenter is free software: you can redistribute it and/or modify
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

import os.path
import pathlib
import shutil


class Versioner:
    def __init__(self):
        here = pathlib.Path(__file__).parent.resolve()
        self.version_file = os.path.abspath(here.parent / 'app' / 'build.gradle.kts')

        self.version_name = None
        self.version_code = None

        self.__parse_versions()

    def __parse_versions(self):
        with open(self.version_file) as f:
            for line in f:
                if 'versionName' in line:
                    separator = '"' if '"' in line else "'"
                    splits = line.split(separator)
                    self.version_name = splits[1]
                    if self.version_code is not None:
                        return

                elif 'versionCode' in line:
                    self.version_code = int(line.strip().split(' ')[-1])
                    if self.version_name is not None:
                        return

    def __save_version(self):
        tmp_file = f'{self.version_file}.tmp'
        with open(self.version_file) as fread:
            with open(tmp_file, 'w') as fwrite:
                for line in fread:
                    if 'versionName' in line.split(' '):
                        separator = '"' if '"' in line else "'"
                        splits = line.split(separator)
                        splits[1] = self.version_name
                        line = separator.join(splits)
                    elif 'versionCode' in line:
                        splits = line.split(' ')
                        splits[-1] = f'{self.version_code}\n'
                        line = ' '.join(splits)
                    fwrite.write(line)
        shutil.move(tmp_file, self.version_file)

    def increment_major(self):
        version_name = self.version_name.split('.')
        version_name = (str(int(version_name[0]) + 1), '0', '0')
        self.version_name = '.'.join(version_name)
        self.version_code += 1000
        self.__save_version()

    def increment_minor(self):
        version_name = self.version_name.split('.')
        version_name = (version_name[0], str(int(version_name[1]) + 1), '0')
        self.version_name = '.'.join(version_name)
        self.version_code += 100
        self.__save_version()

    def increment_patch(self):
        version_name = self.version_name.split('.')
        version_name[2] = str(int(version_name[2]) + 1)
        self.version_name = '.'.join(version_name)
        self.version_code += 10
        self.__save_version()
