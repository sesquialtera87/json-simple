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

import com.json_simple.JsonException.*
import com.json_simple.Yytoken.*
import com.json_simple.Yytoken.Types
import java.io.*
import kotlin.Throws
import java.lang.NullPointerException
import java.lang.StringBuilder
import java.lang.IllegalArgumentException
import java.util.*

/** Jsoner provides JSON utilities for escaping strings to be JSON compatible, thread safe parsing (RFC 7159) JSON
 * strings, and thread safe serializing data to strings in JSON format.
 * @since 2.0.0
 */
object Jsoner {
    /** Deserializes a readable stream according to the RFC 7159 JSON specification.
     * @param readableDeserializable representing content to be deserialized as JSON.
     * @return either a boolean, null, Number, String, JsonObject, or JsonArray that best represents the deserializable.
     * @throws com.github.cliftonlabs.json_simple.JsonException if an unexpected token is encountered in the deserializable. To recover from a
     * JsonException: fix the deserializable to no longer have an unexpected token and try again.
     */
    @Throws(JsonException::class)
    fun deserialize(readableDeserializable: Reader): Any? {
        return deserialize(
            readableDeserializable,
            EnumSet.of(
                DeserializationOptions.ALLOW_JSON_ARRAYS,
                DeserializationOptions.ALLOW_JSON_OBJECTS,
                DeserializationOptions.ALLOW_JSON_DATA
            )
        )[0]
    }

    /** Deserialize a stream with all deserialized JSON values are wrapped in a JsonArray.
     * @param deserializable representing content to be deserialized as JSON.
     * @param flags representing the allowances and restrictions on deserialization.
     * @return the allowable object best represented by the deserializable.
     * @throws com.github.cliftonlabs.json_simple.JsonException if a disallowed or unexpected token is encountered in the deserializable. To recover from a
     * JsonException: fix the deserializable to no longer have a disallowed or unexpected token and try
     * again.
     */
    @Throws(JsonException::class)
    private fun deserialize(deserializable: Reader, flags: Set<DeserializationOptions>): JsonArray {
        val lexer = Yylex(deserializable)
        var token: Yytoken
        var currentState: States
        var returnCount = 1
        val stateStack = LinkedList<States>()
        val valueStack = LinkedList<Any>()
        stateStack.addLast(States.INITIAL)

        do {
            /* Parse through the parsable string's tokens. */
            currentState = popNextState(stateStack)
            token = lexNextToken(lexer)

            when (currentState) {
                States.DONE -> {
                    /* The parse has finished a JSON value. */if (!flags.contains(
                            DeserializationOptions.ALLOW_CONCATENATED_JSON_VALUES
                        ) || Types.END == token.type
                    ) {
                        /* Break if concatenated values are not allowed or if an END token is read. */
                        break
                    }
                    /* Increment the amount of returned JSON values and treat the token as if it were a fresh parse. */returnCount += 1
                    when (token.type) {
                        Types.DATUM ->                            /* A boolean, null, Number, or String could be detected. */if (flags.contains(
                                DeserializationOptions.ALLOW_JSON_DATA
                            )
                        ) {
                            valueStack.addLast(token.value)
                            stateStack.addLast(States.DONE)
                        } else {
                            throw JsonException(lexer.position, Problems.DISALLOWED_TOKEN, token)
                        }
                        Types.LEFT_BRACE ->                            /* An object is detected. */if (flags.contains(
                                DeserializationOptions.ALLOW_JSON_OBJECTS
                            )
                        ) {
                            valueStack.addLast(JsonObject())
                            stateStack.addLast(States.PARSING_OBJECT)
                        } else {
                            throw JsonException(lexer.position, Problems.DISALLOWED_TOKEN, token)
                        }
                        Types.LEFT_SQUARE ->                            /* An array is detected. */if (flags.contains(
                                DeserializationOptions.ALLOW_JSON_ARRAYS
                            )
                        ) {
                            valueStack.addLast(JsonArray())
                            stateStack.addLast(States.PARSING_ARRAY)
                        } else {
                            throw JsonException(lexer.position, Problems.DISALLOWED_TOKEN, token)
                        }
                        else -> throw JsonException(lexer.position, Problems.UNEXPECTED_TOKEN, token)
                    }
                }
                States.INITIAL -> when (token.type) {
                    Types.DATUM -> if (flags.contains(DeserializationOptions.ALLOW_JSON_DATA)) {
                        valueStack.addLast(token.value)
                        stateStack.addLast(States.DONE)
                    } else throw JsonException(lexer.position, Problems.DISALLOWED_TOKEN, token)

                    Types.LEFT_BRACE -> if (flags.contains(DeserializationOptions.ALLOW_JSON_OBJECTS)) {
                        valueStack.addLast(JsonObject())
                        stateStack.addLast(States.PARSING_OBJECT)
                    } else throw JsonException(lexer.position, Problems.DISALLOWED_TOKEN, token)

                    Types.LEFT_SQUARE -> if (flags.contains(DeserializationOptions.ALLOW_JSON_ARRAYS)) {
                        valueStack.addLast(JsonArray())
                        stateStack.addLast(States.PARSING_ARRAY)
                    } else throw JsonException(lexer.position, Problems.DISALLOWED_TOKEN, token)
                    else -> throw JsonException(lexer.position, Problems.UNEXPECTED_TOKEN, token)
                }
                States.PARSED_ERROR -> throw JsonException(
                    lexer.position,
                    Problems.UNEXPECTED_TOKEN,
                    token
                )
                States.PARSING_ARRAY -> when (token.type) {
                    /* The parse could detect a comma while parsing an array since it separates each element. */
                    Types.COMMA -> stateStack.addLast(
                        currentState
                    )
                    Types.DATUM -> {
                        /* The parse found an element of the array. */
                        val jsonArray = valueStack.last as JsonArray
                        jsonArray.add(token.value)
                        stateStack.addLast(currentState)
                    }
                    Types.LEFT_BRACE -> {
                        /* The parse found an object in the array. */
                        val jsonArray = valueStack.last as JsonArray
                        val `object` = JsonObject()
                        jsonArray.add(`object`)
                        valueStack.addLast(`object`)
                        stateStack.addLast(currentState)
                        stateStack.addLast(States.PARSING_OBJECT)
                    }
                    Types.LEFT_SQUARE -> {
                        /* The parse found another array in the array. */
                        val jsonArray = valueStack.last as JsonArray
                        val array = JsonArray()
                        jsonArray.add(array)
                        valueStack.addLast(array)
                        stateStack.addLast(currentState)
                        stateStack.addLast(States.PARSING_ARRAY)
                    }
                    /* The parse found the end of the array. */
                    Types.RIGHT_SQUARE -> if (valueStack.size > returnCount) {
                        valueStack.removeLast()
                    } else {
                        /* The parse has been fully resolved. */
                        stateStack.addLast(States.DONE)
                    }
                    else -> throw JsonException(lexer.position, Problems.UNEXPECTED_TOKEN, token)
                }
                States.PARSING_OBJECT -> when (token.type) {
                    Types.COMMA ->                            /* The parse could detect a comma while parsing an object since it separates each key value
							 * pair. Continue parsing the object. */stateStack.addLast(currentState)
                    Types.DATUM ->                            /* The token ought to be a key. */if (token.value is String) {
                        /* JSON keys are always strings, strings are not always JSON keys but it is going to be
								 * treated as one. Continue parsing the object. */
                        val key = token.value as String
                        valueStack.addLast(key)
                        stateStack.addLast(currentState)
                        stateStack.addLast(States.PARSING_ENTRY)
                    } else {
                        /* Abort! JSON keys are always strings and it wasn't a string. */
                        throw JsonException(lexer.position, Problems.UNEXPECTED_TOKEN, token)
                    }
                    Types.RIGHT_BRACE ->                            /* The parse has found the end of the object. */if (valueStack.size > returnCount) {
                        /* There are unresolved values remaining. */
                        valueStack.removeLast()
                    } else {
                        /* The parse has been fully resolved. */
                        stateStack.addLast(States.DONE)
                    }
                    else -> throw JsonException(lexer.position, Problems.UNEXPECTED_TOKEN, token)
                }
                States.PARSING_ENTRY -> when (token.type) {
                    Types.COLON ->                            /* The parse could detect a colon while parsing a key value pair since it separates the key
							 * and value from each other. Continue parsing the entry. */stateStack.addLast(currentState)
                    Types.DATUM -> {
                        /* The parse has found a value for the parsed pair key. */
                        val key = valueStack.removeLast() as String
                        val parent = valueStack.last as JsonObject
                        parent[key] = token.value
                    }
                    Types.LEFT_BRACE -> {
                        /* The parse has found an object for the parsed pair key. */
                        val key = valueStack.removeLast() as String
                        val parent = valueStack.last as JsonObject
                        val `object` = JsonObject()
                        parent[key] = `object`
                        valueStack.addLast(`object`)
                        stateStack.addLast(States.PARSING_OBJECT)
                    }
                    Types.LEFT_SQUARE -> {
                        /* The parse has found an array for the parsed pair key. */
                        val key = valueStack.removeLast() as String
                        val parent = valueStack.last as JsonObject
                        val array = JsonArray()
                        parent[key] = array
                        valueStack.addLast(array)
                        stateStack.addLast(States.PARSING_ARRAY)
                    }
                    else -> throw JsonException(lexer.position, Problems.UNEXPECTED_TOKEN, token)
                }
                else -> {
                }
            }
            /* If we're not at the END and DONE then do the above again. */
        } while (!(States.DONE == currentState && Types.END == token.type))
        return JsonArray(valueStack)
    }

    /** A convenience method that assumes a StringReader to deserialize a string.
     * @param deserializable representing content to be deserialized as JSON.
     * @return either a boolean, null, Number, String, JsonObject, or JsonArray that best represents the deserializable.
     * @throws com.github.cliftonlabs.json_simple.JsonException if an unexpected token is encountered in the deserializable. To recover from a
     * JsonException: fix the deserializable to no longer have an unexpected token and try again.
     * @see Jsoner.deserialize
     * @see StringReader
     */
    @Throws(JsonException::class)
    fun deserialize(deserializable: String?): Any? {
        var returnable: Any?
        var readableDeserializable: StringReader? = null
        try {
            readableDeserializable = StringReader(deserializable)
            returnable = deserialize(readableDeserializable)
        } catch (caught: NullPointerException) {
            /* They both have the same recovery scenario.
			 * See StringReader.
			 * If deserializable is null, it should be reasonable to expect null back. */
            returnable = null
        } finally {
            readableDeserializable?.close()
        }
        return returnable
    }

    /** A convenience method that assumes a JsonArray must be deserialized.
     * @param deserializable representing content to be deserializable as a JsonArray.
     * @param defaultValue representing what would be returned if deserializable isn't a JsonArray or an IOException,
     * NullPointerException, or JsonException occurs during deserialization.
     * @return a JsonArray that represents the deserializable, or the defaultValue if there isn't a JsonArray that
     * represents deserializable.
     * @see Jsoner.deserialize
     */
    fun deserialize(deserializable: String?, defaultValue: JsonArray?): JsonArray? {
        var readable: StringReader? = null
        var returnable: JsonArray?
        try {
            readable = StringReader(deserializable)
            returnable = deserialize(readable, EnumSet.of(DeserializationOptions.ALLOW_JSON_ARRAYS)).getCollection(0)
        } catch (caught: NullPointerException) {
            /* Don't care, just return the default value. */
            returnable = defaultValue
        } catch (caught: JsonException) {
            returnable = defaultValue
        } finally {
            readable?.close()
        }
        return returnable
    }

    /** A convenience method that assumes a JsonObject must be deserialized.
     * @param deserializable representing content to be deserializable as a JsonObject.
     * @param defaultValue representing what would be returned if deserializable isn't a JsonObject or an IOException,
     * NullPointerException, or JsonException occurs during deserialization.
     * @return a JsonObject that represents the deserializable, or the defaultValue if there isn't a JsonObject that
     * represents deserializable.
     * @see Jsoner.deserialize
     */
    private fun deserialize(deserializable: String?, defaultValue: JsonObject?): JsonObject? {
        var readable: StringReader? = null
        var returnable: JsonObject?
        try {
            readable = StringReader(deserializable)
            returnable = deserialize(readable, EnumSet.of(DeserializationOptions.ALLOW_JSON_OBJECTS)).getMap(0)
        } catch (caught: NullPointerException) {
            /* Don't care, just return the default value. */
            returnable = defaultValue
        } catch (caught: JsonException) {
            returnable = defaultValue
        } finally {
            readable?.close()
        }
        return returnable
    }

    /** A convenience method that assumes multiple RFC 7159 JSON values (except numbers) have been concatenated together
     * for deserilization which will be collectively returned in a JsonArray wrapper.
     * There may be numbers included, they just must not be concatenated together as it is prone to
     * NumberFormatExceptions (thus causing a JsonException) or the numbers no longer represent their
     * respective values.
     * Examples:
     * "123null321" returns [123, null, 321]
     * "nullnullnulltruefalse\"\"{}[]" returns [null, null, null, true, false, "", {}, []]
     * "123" appended to "321" returns [123321]
     * "12.3" appended to "3.21" throws JsonException(NumberFormatException)
     * "123" appended to "-321" throws JsonException(NumberFormatException)
     * "123e321" appended to "-1" throws JsonException(NumberFormatException)
     * "null12.33.21null" throws JsonException(NumberFormatException)
     * @param deserializable representing concatenated content to be deserialized as JSON in one reader. Its contents
     * may not contain two numbers concatenated together.
     * @return a JsonArray that contains each of the concatenated objects as its elements. Each concatenated element is
     * either a boolean, null, Number, String, JsonArray, or JsonObject that best represents the concatenated
     * content inside deserializable.
     * @throws com.github.cliftonlabs.json_simple.JsonException if an unexpected token is encountered in the deserializable. To recover from a
     * JsonException: fix the deserializable to no longer have an unexpected token and try again.
     */
    @Throws(JsonException::class)
    fun deserializeMany(deserializable: Reader): JsonArray {
        return deserialize(
            deserializable,
            EnumSet.of(
                DeserializationOptions.ALLOW_JSON_ARRAYS,
                DeserializationOptions.ALLOW_JSON_OBJECTS,
                DeserializationOptions.ALLOW_JSON_DATA,
                DeserializationOptions.ALLOW_CONCATENATED_JSON_VALUES
            )
        )
    }

    /** Escapes potentially confusing or important characters in the String provided.
     * @param escapable an unescaped string.
     * @return an escaped string for usage in JSON; An escaped string is one that has escaped all of the quotes ("),
     * backslashes (\), return character (\r), new line character (\n), tab character (\t),
     * backspace character (\b), form feed character (\f) and other control characters [\u0000] [\u001F] or
     * characters \u007F \u009F, \u2000 \u20FF with a
     * backslash (\) which itself must be escaped by the backslash in a java string.
     */
    fun escape(escapable: String): String {
        val builder = StringBuilder()
        val characters = escapable.length
        for (i in 0 until characters) {
            val character = escapable[i]
            when (character) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                '/' -> builder.append("\\/")
                else ->                    /* The many characters that get replaced are benign to software but could be mistaken by people
					 * reading it for a JSON relevant character. */if (character >= '\u0000' && character <= '\u001F' || character >= '\u007F' && character <= '\u009F' || character >= '\u2000' && character <= '\u20FF') {
                    val characterHexCode = Integer.toHexString(character.toInt())
                    builder.append("\\u")
                    var k = 0
                    while (k < 4 - characterHexCode.length) {
                        builder.append("0")
                        k++
                    }
                    builder.append(characterHexCode.toUpperCase())
                } else {
                    /* Character didn't need escaping. */
                    builder.append(character)
                }
            }
        }
        return builder.toString()
    }

    /** Processes the lexer's reader for the next token.
     * @param lexer represents a text processor being used in the deserialization process.
     * @return a token representing a meaningful element encountered by the lexer.
     * @throws com.github.cliftonlabs.json_simple.JsonException if an unexpected character is encountered while processing the text.
     */
    @Throws(JsonException::class)
    private fun lexNextToken(lexer: Yylex): Yytoken {
        var returnable: Yytoken?
        /* Parse through the next token. */returnable = try {
            lexer.yylex()
        } catch (caught: IOException) {
            throw JsonException(-1, Problems.UNEXPECTED_EXCEPTION, caught)
        }
        if (returnable == null) {
            /* If there isn't another token, it must be the end. */
            returnable = Yytoken(Types.END, null)
        }
        return returnable
    }

    /** Creates a new JsonKey that wraps the given string and value. This function should NOT be
     * used in favor of existing constants and enumerations to make code easier to maintain.
     * @param key represents the JsonKey as a String.
     * @param value represents the value the JsonKey uses.
     * @return a JsonKey that represents the provided key and value.
     */
    fun mintJsonKey(key: String, value: Any): JsonKey {
        return object : JsonKey {
            override val key: String
                get() = key
            override val value: Any
                get() = value
        }
    }

    /** Used for state transitions while deserializing.
     * @param stateStack represents the deserialization states saved for future processing.
     * @return a state for deserialization context so it knows how to consume the next token.
     */
    private fun popNextState(stateStack: LinkedList<States>): States {
        return if (stateStack.size > 0) {
            stateStack.removeLast()
        } else {
            States.PARSED_ERROR
        }
    }

    /** Makes the JSON input more easily human readable using indentation and newline of the caller's choice. This means
     * the validity of the JSON printed by this method is dependent on the caller's choice of indentation and newlines.
     * @param readable representing a JSON formatted string with out extraneous characters, like one returned from
     * Jsoner#serialize(Object).
     * @param writable represents where the pretty printed JSON should be written to.
     * @param indentation representing the indentation used to format the JSON string. NOT validated as a proper
     * indentation. It is recommended to use tabs ("\t"), but 3, 4, or 8 spaces are common alternatives.
     * @param newline representing the newline used to format the JSON string. NOT validated as a proper newline. It is
     * recommended to use "\n", but "\r" or "/r/n" are common alternatives.
     * @throws IOException if the provided writer encounters an IO issue.
     * @throws com.github.cliftonlabs.json_simple.JsonException if the provided reader encounters an IO issue.
     * @see Jsoner.prettyPrint
     * @since 3.1.0 made public to allow large JSON inputs and more pretty print control.
     */
    @Throws(IOException::class, JsonException::class)
    fun prettyPrint(readable: Reader?, writable: Writer, indentation: String?, newline: String?) {
        val lexer = Yylex(readable)
        var lexed: Yytoken
        var level = 0
        do {
            lexed = lexNextToken(lexer)
            when (lexed.type) {
                Types.COLON -> writable.append(lexed.value.toString())
                Types.COMMA -> {
                    writable.append(lexed.value.toString())
                    writable.append(newline)
                    var i = 0
                    while (i < level) {
                        writable.append(indentation)
                        i++
                    }
                }
                Types.END -> {
                }
                Types.LEFT_BRACE, Types.LEFT_SQUARE -> {
                    writable.append(lexed.value.toString())
                    writable.append(newline)
                    level++
                    var i = 0
                    while (i < level) {
                        writable.append(indentation)
                        i++
                    }
                }
                Types.RIGHT_BRACE, Types.RIGHT_SQUARE -> {
                    writable.append(newline)
                    level--
                    var i = 0
                    while (i < level) {
                        writable.append(indentation)
                        i++
                    }
                    writable.append(lexed.value.toString())
                }
                else -> if (lexed.value == null) {
                    writable.append("null")
                } else if (lexed.value is String) {
                    writable.append("\"")
                    writable.append(escape(lexed.value as String))
                    writable.append("\"")
                } else {
                    writable.append(lexed.value.toString())
                }
            }
        } while (lexed.type != Types.END)
        writable.flush()
    }

    /** A convenience method to pretty print a String with tabs ("\t") and "\n" for newlines.
     * @param printable representing a JSON formatted string with out extraneous characters, like one returned from
     * Jsoner#serialize(Object).
     * @return printable except it will have '\n' then '\t' characters inserted after '[', '{', ',' and before ']' '}'
     * tokens in the JSON. It will return null if printable isn't a JSON string.
     */
    fun prettyPrint(printable: String?): String {
        val writer = StringWriter()
        try {
            prettyPrint(StringReader(printable), writer, "\t", "\n")
        } catch (caught: IOException) {
            /* See java.io.StringReader.
			 * See java.io.StringWriter. */
        } catch (caught: JsonException) {
            /* Would have been caused by a an IO exception while lexing, but the StringReader does not throw them. See
			 * java.io.StringReader. */
        }
        return writer.toString()
    }

    /** A convenience method to pretty print a String with the provided spaces count and "\n" for newlines.
     * @param printable representing a JSON formatted string with out extraneous characters, like one returned from
     * Jsoner#serialize(Object).
     * @param spaces representing the amount of spaces to use for indentation. Must be between 2 and 10.
     * @return printable except it will have '\n' then space characters inserted after '[', '{', ',' and before ']' '}'
     * tokens in the JSON. It will return null if printable isn't a JSON string.
     * @throws IllegalArgumentException if spaces isn't between [2..10].
     * @see Jsoner.prettyPrint
     * @since 2.2.0 to allow pretty printing with spaces instead of tabs.
     */
    @Deprecated(
        """3.1.0 in favor of Jsoner#prettyPrint(Reader, Writer, String, String) due to arbitrary limitations
	              enforced by this implementation. """
    )
    fun prettyPrint(printable: String?, spaces: Int): String {
        require(!(spaces > 10 || spaces < 2)) { "Indentation with spaces must be between 2 and 10." }
        val indentation = StringBuilder("")
        val writer = StringWriter()
        for (i in 0 until spaces) {
            indentation.append(" ")
        }
        try {
            prettyPrint(StringReader(printable), writer, indentation.toString(), "\n")
        } catch (caught: IOException) {
            /* See java.io.StringReader.
			 * See java.io.StringWriter. */
        } catch (caught: JsonException) {
            /* Would have been caused by a an IO exception while lexing, but the StringReader does not throw them. See
			 * java.io.StringReader. */
        }
        return writer.toString()
    }

    /** A convenience method that assumes a StringWriter.
     * @param jsonSerializable represents the object that should be serialized as a string in JSON format.
     * @return a string, in JSON format, that represents the object provided.
     * @throws IllegalArgumentException if the jsonSerializable isn't serializable in JSON.
     * @see Jsoner.serialize
     * @see StringWriter
     */
    fun serialize(jsonSerializable: Any?): String {
        val writableDestination = StringWriter()
        try {
            serialize(jsonSerializable, writableDestination)
        } catch (caught: IOException) {
            /* See java.io.StringWriter. */
        }
        return writableDestination.toString()
    }

    /** Serializes values according to the RFC 7159 JSON specification. It will also trust the serialization provided by
     * any Jsonables it serializes.
     * @param jsonSerializable represents the object that should be serialized in JSON format.
     * @param writableDestination represents where the resulting JSON text is written to.
     * @throws IOException if the writableDestination encounters an I/O problem, like being closed while in use.
     * @throws IllegalArgumentException if the jsonSerializable isn't serializable in JSON.
     */
    @Throws(IOException::class)
    fun serialize(jsonSerializable: Any?, writableDestination: Writer) {
        serialize(jsonSerializable, writableDestination, EnumSet.of(SerializationOptions.ALLOW_JSONABLES))
    }

    /** Serialize values to JSON and write them to the provided writer based on behavior flags.
     * @param jsonSerializable represents the object that should be serialized to a string in JSON format.
     * @param writableDestination represents where the resulting JSON text is written to.
     * @param flags represents the allowances and restrictions on serialization.
     * @throws IOException if the writableDestination encounters an I/O problem.
     * @throws IllegalArgumentException if the jsonSerializable isn't serializable in JSON.
     * @see SerializationOptions
     */
    @Throws(IOException::class)
    private fun serialize(jsonSerializable: Any?, writableDestination: Writer, flags: Set<SerializationOptions>) {
        if (jsonSerializable == null) {
            /* When a null is passed in the word null is supported in JSON. */
            writableDestination.write("null")
        } else if (jsonSerializable is Jsonable && flags.contains(SerializationOptions.ALLOW_JSONABLES)) {
            /* Writes the writable as defined by the writable. */
            jsonSerializable.toJson(writableDestination)
        } else if (jsonSerializable is String) {
            /* Make sure the string is properly escaped. */
            writableDestination.write('"'.toInt())
            writableDestination.write(escape(jsonSerializable))
            writableDestination.write('"'.toInt())
        } else if (jsonSerializable is Char) {
            /* Make sure the string is properly escaped.
			 * Quotes for some reason are necessary for String, but not Character. */
            writableDestination.write(escape(jsonSerializable.toString()))
        } else if (jsonSerializable is Double) {
            if (jsonSerializable.isInfinite() || jsonSerializable.isNaN()) {
                /* Infinite and not a number are not supported by the JSON specification, so null is used instead. */
                writableDestination.write("null")
            } else {
                writableDestination.write(jsonSerializable.toString())
            }
        } else if (jsonSerializable is Float) {
            if (jsonSerializable.isInfinite() || jsonSerializable.isNaN()) {
                /* Infinite and not a number are not supported by the JSON specification, so null is used instead. */
                writableDestination.write("null")
            } else {
                writableDestination.write(jsonSerializable.toString())
            }
        } else if (jsonSerializable is Number) {
            writableDestination.write(jsonSerializable.toString())
        } else if (jsonSerializable is Boolean) {
            writableDestination.write(jsonSerializable.toString())
        } else if (jsonSerializable is Map<*, *>) {
            /* Writes the map in JSON object format. */
            var isFirstEntry = true
            val entries: Iterator<*> = jsonSerializable.entries.iterator()
            writableDestination.write('{'.toInt())
            while (entries.hasNext()) {
                if (isFirstEntry) {
                    isFirstEntry = false
                } else {
                    writableDestination.write(','.toInt())
                }
                val entry = entries.next() as Map.Entry<*, *>
                serialize(entry.key, writableDestination, flags)
                writableDestination.write(':'.toInt())
                serialize(entry.value, writableDestination, flags)
            }
            writableDestination.write('}'.toInt())
        } else if (jsonSerializable is Collection<*>) {
            /* Writes the collection in JSON array format. */
            var isFirstElement = true
            val elements = jsonSerializable.iterator()
            writableDestination.write('['.toInt())
            while (elements.hasNext()) {
                if (isFirstElement) {
                    isFirstElement = false
                } else {
                    writableDestination.write(','.toInt())
                }
                serialize(elements.next(), writableDestination, flags)
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is ByteArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write(','.toInt())
                }
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is ShortArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write(','.toInt())
                }
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is IntArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write(','.toInt())
                }
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is LongArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write(','.toInt())
                }
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is FloatArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write(','.toInt())
                }
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is DoubleArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write(','.toInt())
                }
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is BooleanArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write(','.toInt())
                }
            }
            writableDestination.write(']'.toInt())
        } else if (jsonSerializable is CharArray) {
            /* Writes the array in JSON array format. */
            val writableArray = jsonSerializable
            val numberOfElements = writableArray.size
            writableDestination.write("[\"")
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(writableArray[i], writableDestination, flags)
                } else {
                    serialize(writableArray[i], writableDestination, flags)
                    writableDestination.write("\",\"")
                }
            }
            writableDestination.write("\"]")
        } else if (jsonSerializable is Array<*>) {
            /* Writes the array in JSON array format. */
            val numberOfElements = jsonSerializable.size
            writableDestination.write('['.toInt())
            for (i in 0 until numberOfElements) {
                if (i == numberOfElements - 1) {
                    serialize(jsonSerializable[i], writableDestination, flags)
                } else {
                    serialize(jsonSerializable[i], writableDestination, flags)
                    writableDestination.write(",")
                }
            }
            writableDestination.write(']'.toInt())
        } else {
            /* It cannot by any measure be safely serialized according to specification. */
            if (flags.contains(SerializationOptions.ALLOW_INVALIDS)) {
                /* Can be helpful for debugging how it isn't valid. */
                writableDestination.write(jsonSerializable.toString())
            } else {
                /* Notify the caller the cause of failure for the serialization. */
                throw IllegalArgumentException(
                    """Encountered a: ${jsonSerializable.javaClass.name} as: $jsonSerializable  that isn't JSON serializable.
  Try:
    1) Implementing the Jsonable interface for the object to return valid JSON. If it already does it probably has a bug.
    2) If you cannot edit the source of the object or couple it with this library consider wrapping it in a class that does implement the Jsonable interface.
    3) Otherwise convert it to a boolean, null, number, JsonArray, JsonObject, or String value before serializing it.
    4) If you feel it should have serialized you could use a more tolerant serialization for debugging purposes."""
                )
            }
        }
    }

    /** Serializes like the first version of this library.
     * It has been adapted to use Jsonable for serializing custom objects, but otherwise works like the old JSON string
     * serializer. It will allow non-JSON values in its output like the old one. It can be helpful for last resort log
     * statements and debugging errors in self generated JSON. Anything serialized using this method isn't guaranteed to
     * be deserializable.
     * @param jsonSerializable represents the object that should be serialized in JSON format.
     * @param writableDestination represents where the resulting JSON text is written to.
     * @throws IOException if the writableDestination encounters an I/O problem, like being closed while in use.
     */
    @Throws(IOException::class)
    fun serializeCarelessly(jsonSerializable: Any?, writableDestination: Writer) {
        serialize(
            jsonSerializable,
            writableDestination,
            EnumSet.of(SerializationOptions.ALLOW_JSONABLES, SerializationOptions.ALLOW_INVALIDS)
        )
    }

    /** Serializes JSON values and only JSON values according to the RFC 7159 JSON specification.
     * @param jsonSerializable represents the object that should be serialized in JSON format.
     * @param writableDestination represents where the resulting JSON text is written to.
     * @throws IOException if the writableDestination encounters an I/O problem, like being closed while in use.
     * @throws IllegalArgumentException if the jsonSerializable isn't serializable in raw JSON.
     */
    @Throws(IOException::class)
    fun serializeStrictly(jsonSerializable: Any?, writableDestination: Writer) {
        serialize(jsonSerializable, writableDestination, EnumSet.noneOf(SerializationOptions::class.java))
    }

    /** Flags to tweak the behavior of the primary deserialization method.  */
    private enum class DeserializationOptions {
        /** Whether multiple JSON values can be deserialized as a root element.  */
        ALLOW_CONCATENATED_JSON_VALUES,

        /** Whether a JsonArray can be deserialized as a root element.  */
        ALLOW_JSON_ARRAYS,

        /** Whether a boolean, null, Number, or String can be deserialized as a root element.  */
        ALLOW_JSON_DATA,

        /** Whether a JsonObject can be deserialized as a root element.  */
        ALLOW_JSON_OBJECTS
    }

    /** Flags to tweak the behavior of the primary serialization method.  */
    private enum class SerializationOptions {
        /** Instead of aborting serialization on non-JSON values it will continue serialization by serializing the
         * non-JSON value directly into the now invalid JSON. Be mindful that invalid JSON will not successfully
         * deserialize.  */
        ALLOW_INVALIDS,

        /** Instead of aborting serialization on non-JSON values that implement Jsonable it will continue serialization
         * by deferring serialization to the Jsonable.
         * @see com.github.cliftonlabs.json_simple.Jsonable
         */
        ALLOW_JSONABLES
    }

    /** The possible States of a JSON deserializer.  */
    private enum class States {
        /** Post-parsing state.  */
        DONE,

        /** Pre-parsing state.  */
        INITIAL,

        /** Parsing error, ParsingException should be thrown.  */
        PARSED_ERROR, PARSING_ARRAY,

        /** Parsing a key-value pair inside of an object.  */
        PARSING_ENTRY, PARSING_OBJECT
    }
}