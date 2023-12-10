/*
 * DocumenterApplication.kt
 *
 * Copyright (C) 2022 ViliusSutkus89.com
 *
 * Documenter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viliussutkus89.documenter

import android.util.Log
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.viliussutkus89.documenter.model.DocumentDatabase


class DocumenterApplication: MultiDexApplication(), Configuration.Provider {
    val documentDatabase: DocumentDatabase by lazy {
        DocumentDatabase.getDatabase(this)
    }

    override val workManagerConfiguration: Configuration get() = Configuration.Builder()
        .setDefaultProcessName(packageName)
        .setMinimumLoggingLevel(Log.INFO)
        .build()
}
