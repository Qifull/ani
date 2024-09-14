/*
 * Ani
 * Copyright (C) 2022-2024 Him188
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `ani-mpp-lib-targets`
    idea
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlinx.serialization.core)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(projects.utils.ktorClient)
            api(projects.utils.serialization)
            implementation(projects.utils.platform)
            api(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            implementation(projects.utils.logging)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.utils.testing)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            api(libs.jsoup)
        }
    }
}

idea {
    module.generatedSourceDirs.add(file("test/title/generated"))
}