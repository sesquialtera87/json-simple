/* Copyright 2016 Clifton Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.json_simple

import com.json_simple.Yytoken.Types.*
import java.lang.StringBuilder

/** Represents structural entities in JSON.
 * @since 2.0.0
 */
internal class Yytoken(type: Types, value: Any?) {
    /** Represents the different kinds of tokens.  */
    internal enum class Types {
        /** Tokens of this type will always have a value of ":"  */
        COLON,

        /** Tokens of this type will always have a value of ","  */
        COMMA,

        /** Tokens of this type will always have a value that is a boolean, null, number, or string.  */
        DATUM,

        /** Tokens of this type will always have a value of ""  */
        END,

        /** Tokens of this type will always have a value of "{"  */
        LEFT_BRACE,

        /** Tokens of this type will always have a value of "["  */
        LEFT_SQUARE,

        /** Tokens of this type will always have a value of "}"  */
        RIGHT_BRACE,

        /** Tokens of this type will always have a value of "]"  */
        RIGHT_SQUARE
    }

    /** @return which of the [Types] the token is.
     * @see Types
     */
    val type: Types

    /** @return what the token is.
     * @see Types
     */
    var value: Any? = null

    override fun toString() =
        StringBuilder()
            .append(type.toString())
            .append("(")
            .append(value)
            .append(")")
            .toString()

    /** @param type represents the kind of token the instantiated token will be.
     * @param value represents the value the token is associated with, will be ignored unless type is equal to
     * Types.DATUM.
     * @see Types
     */
    init {
        /* Sanity check. Make sure the value is ignored for the proper value unless it is a datum token. */
        when (type) {
            COLON -> this.value = ":"
            COMMA -> this.value = ","
            END -> this.value = ""
            LEFT_BRACE -> this.value = "{"
            LEFT_SQUARE -> this.value = "["
            RIGHT_BRACE -> this.value = "}"
            RIGHT_SQUARE -> this.value = "]"
            else -> this.value = value
        }
        this.type = type
    }
}