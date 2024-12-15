/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package me.him188.ani.datasources.bangumi.next.models

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param actors
 * @param character
 * @param order
 * @param type
 */
@Serializable

data class BangumiNextSubjectCharacter(

    @SerialName(value = "actors") @Required val actors: kotlin.collections.List<BangumiNextSlimPerson>,

    @SerialName(value = "character") @Required val character: BangumiNextSlimCharacter,

    @SerialName(value = "order") @Required val order: kotlin.Int,

    @SerialName(value = "type") @Required val type: kotlin.Int

)
