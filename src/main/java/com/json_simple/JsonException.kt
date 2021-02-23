/* Copyright 2016-2017 Clifton Labs
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

import com.json_simple.JsonException.Problems.*
import java.lang.Exception
import java.lang.StringBuilder

/** JsonException explains how and where the problem occurs in the source JSON text during deserialization.
 * @since 3.0.0
 */
class JsonException(
    /** Helps debug the location of a problem.
     * @return an index of the string character the error type occurred at.
     */
    val position: Long,
    /** Helps find an appropriate solution for a problem.
     * @return the enumeration for how the exception occurred.
     */
    val problemType: Problems,
    /** Helps identify the problem.
     * @return a representation of what caused the exception.
     */
    val unexpectedObject: Any
) : Exception() {
    /** The kinds of exceptions that can trigger a JsonException.  */
    enum class Problems {
        DISALLOWED_TOKEN,

        /** @since 2.3.0 to consolidate exceptions that occur during deserialization.
         */
        IOEXCEPTION, UNEXPECTED_CHARACTER, UNEXPECTED_EXCEPTION, UNEXPECTED_TOKEN
    }

    override val message: String
        get() {
            val sb = StringBuilder()
            when (problemType) {
                DISALLOWED_TOKEN -> sb.append("The disallowed token (").append(
                    unexpectedObject
                ).append(") was found at position ").append(position)
                    .append(". If this is in error, try again with a deserialization method in Jsoner that allows the token instead. Otherwise, fix the parsable string and try again.")
                IOEXCEPTION -> sb.append("An IOException was encountered, ensure the reader is properly instantiated, isn't closed, or that it is ready before trying again.\n")
                    .append(
                        unexpectedObject
                    )
                UNEXPECTED_CHARACTER -> sb.append("The unexpected character (").append(
                    unexpectedObject
                ).append(") was found at position ").append(position).append(". Fix the parsable string and try again.")
                UNEXPECTED_TOKEN -> sb.append("The unexpected token ").append(
                    unexpectedObject
                ).append(" was found at position ").append(position).append(". Fix the parsable string and try again.")
                UNEXPECTED_EXCEPTION -> sb.append("Please report this to the library's maintainer. The unexpected exception that should be addressed before trying again occurred at position ")
                    .append(
                        position
                    ).append(":\n").append(unexpectedObject)
            }
            return sb.toString()
        }

    companion object {
        private const val serialVersionUID = 1L
    }

    /** Instantiates a JsonException without assumptions.
     * @param position where the exception occurred.
     * @param problemType how the exception occurred.
     * @param unexpectedObject what caused the exception.
     */
    init {
        if (IOEXCEPTION == problemType || UNEXPECTED_EXCEPTION == problemType) {
            if (unexpectedObject is Throwable) {
                initCause(unexpectedObject)
            }
        }
    }
}