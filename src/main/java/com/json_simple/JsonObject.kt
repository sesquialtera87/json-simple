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

import java.math.BigDecimal
import java.lang.StringBuilder
import java.util.NoSuchElementException
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import kotlin.Throws

/** JsonObject is a common non-thread safe data format for string to data mappings. The contents of a JsonObject are
 * only validated as JSON values on serialization. Meaning all values added to a JsonObject must be recognized by the
 * Jsoner for it to be a true 'JsonObject', so it is really a JsonableHashMap that will serialize to a JsonObject if all
 * of its contents are valid JSON.
 * @see Jsoner
 *
 * @since 2.0.0
 */
class JsonObject : HashMap<String?, Any?>, Jsonable {
    /** Instantiates an empty JsonObject.  */
    constructor() : super()

    /** Instantiate a new JsonObject by accepting a map's entries, which could lead to de/serialization issues of the
     * resulting JsonObject since the entry values aren't validated as JSON values.
     * @param map represents the mappings to produce the JsonObject with.
     */
    constructor(map: Map<String?, *>?) : super(map)

    /** A convenience method that assumes there is a BigDecimal, Number, or String at the given key. If a Number is
     * there its Number#toString() is used to construct a new BigDecimal(String). If a String is there it is used to
     * construct a new BigDecimal(String).
     * @param key representing where the value ought to be paired with.
     * @return a BigDecimal representing the value paired with the key.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see BigDecimal
     *
     * @see Number.toString
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getBigDecimal(key: JsonKey): BigDecimal? {
        var returnable = this[key.key]
        if (returnable is BigDecimal) {
            /* Success there was a BigDecimal or it defaulted. */
        } else if (returnable is Number) {
            /* A number can be used to construct a BigDecimal */
            returnable = BigDecimal(returnable.toString())
        } else if (returnable is String) {
            /* A number can be used to construct a BigDecimal */
            returnable = BigDecimal(returnable as String?)
        }
        return returnable as BigDecimal?
    }

    /** A convenience method that assumes there is a BigDecimal, Number, or String at the given key. If a Number is
     * there its Number#toString() is used to construct a new BigDecimal(String). If a String is there it is used to
     * construct a new BigDecimal(String).
     * @param key representing where the value ought to be paired with.
     * @return a BigDecimal representing the value paired with the key or JsonKey#getValue() if the key isn't present.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see BigDecimal
     *
     * @see Number.toString
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getBigDecimalOrDefault(key: JsonKey): BigDecimal? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }

        when (returnable) {
            is BigDecimal -> {
                /* Success there was a BigDecimal or it defaulted. */
            }
            is Number -> {
                /* A number can be used to construct a BigDecimal */
                returnable = BigDecimal(returnable.toString())
            }
            is String -> {
                /* A String can be used to construct a BigDecimal */
                returnable = BigDecimal(returnable as String?)
            }
        }
        return returnable as BigDecimal?
    }

    /** A convenience method that assumes there is a Boolean or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Boolean representing the value paired with the key.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getBoolean(key: JsonKey): Boolean? {
        var returnable = this[key.key]
        if (returnable is String) {
            returnable = java.lang.Boolean.valueOf(returnable as String?)
        }
        return returnable as Boolean?
    }

    /** A convenience method that assumes there is a Boolean or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Boolean representing the value paired with the key or JsonKey#getValue() if the key isn't present.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getBooleanOrDefault(key: JsonKey): Boolean? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable is String) {
            returnable = java.lang.Boolean.valueOf(returnable as String?)
        }
        return returnable as Boolean?
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Byte representing the value paired with the key (which may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.byteValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getByte(key: JsonKey): Byte? {
        var returnable = this[key.key] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toByte()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Byte representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
     * may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.byteValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getByteOrDefault(key: JsonKey): Byte? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable == null) {
            return null
        }
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable as String?)
        }
        return (returnable as Number).toByte()
    }

    /** A convenience method that assumes there is a Collection at the given key.
     * @param <T> the kind of collection to expect at the key. Note unless manually added, collection values will be a
     * JsonArray.
     * @param key representing where the value ought to be paired with.
     * @return a Collection representing the value paired with the key.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
    </T> */
    fun <T : Collection<*>?> getCollection(key: JsonKey): T? {
        /* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
        return this[key.key] as T?
    }

    /** A convenience method that assumes there is a Collection at the given key.
     * @param <T> the kind of collection to expect at the key. Note unless manually added, collection values will be a
     * JsonArray.
     * @param key representing where the value ought to be paired with.
     * @return a Collection representing the value paired with the key or JsonKey#getValue() if the key isn't present..
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
    </T> */
    fun <T : Collection<*>?> getCollectionOrDefault(key: JsonKey): T? {
        /* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
        val returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        return returnable as T?
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Double representing the value paired with the key (which may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.doubleValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getDouble(key: JsonKey): Double? {
        var returnable = this[key.key] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toDouble()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Double representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
     * may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.doubleValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getDoubleOrDefault(key: JsonKey): Double? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable == null) {
            return null
        }
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable as String?)
        }
        return (returnable as Number).toDouble()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Float representing the value paired with the key (which may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.floatValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getFloat(key: JsonKey): Float? {
        var returnable = this[key.key] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toFloat()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Float representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
     * may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.floatValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getFloatOrDefault(key: JsonKey): Float? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable == null) {
            return null
        }
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable as String?)
        }
        return (returnable as Number).toFloat()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return an Integer representing the value paired with the key (which may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.intValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getInteger(key: JsonKey): Int? {
        var returnable = this[key.key] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toInt()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return an Integer representing the value paired with the key or JsonKey#getValue() if the key isn't present
     * (which may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.intValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getIntegerOrDefault(key: JsonKey): Int? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable == null) {
            return null
        }
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable as String?)
        }
        return (returnable as Number).toInt()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Long representing the value paired with the key (which may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.longValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getLong(key: JsonKey): Long? {
        var returnable = this[key.key] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toLong()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Long representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
     * may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.longValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getLongOrDefault(key: JsonKey): Long? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable == null) {
            return null
        }
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable as String?)
        }
        return (returnable as Number).toLong()
    }

    /** A convenience method that assumes there is a Map at the given key.
     * @param <T> the kind of map to expect at the key. Note unless manually added, Map values will be a JsonObject.
     * @param key representing where the value ought to be paired with.
     * @return a Map representing the value paired with the key.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
    </T> */
    fun <T : Map<*, *>?> getMap(key: JsonKey): T? {
        /* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
        return this[key.key] as T?
    }

    /** A convenience method that assumes there is a Map at the given key.
     * @param <T> the kind of map to expect at the key. Note unless manually added, Map values will be a JsonObject.
     * @param key representing where the value ought to be paired with.
     * @return a Map representing the value paired with the key or JsonKey#getValue() if the key isn't present.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
    </T> */
    fun <T : Map<*, *>?> getMapOrDefault(key: JsonKey): T? {
        /* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
        val returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        return returnable as T?
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Short representing the value paired with the key (which may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.shortValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getShort(key: JsonKey): Short? {
        var returnable = this[key.key] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toShort()
    }

    /** A convenience method that assumes there is a Number or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a Short representing the value paired with the key or JsonKey#getValue() if the key isn't present (which
     * may involve rounding or truncation).
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @see Number.shortValue
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getShortOrDefault(key: JsonKey): Short? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable == null) {
            return null
        }
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable as String?)
        }
        return (returnable as Number).toShort()
    }

    /** A convenience method that assumes there is a Boolean, Number, or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a String representing the value paired with the key.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getString(key: JsonKey): String? {
        var returnable = this[key.key]
        if (returnable is Boolean) {
            returnable = returnable.toString()
        } else if (returnable is Number) {
            returnable = returnable.toString()
        }
        return returnable as String?
    }

    /** A convenience method that assumes there is a Boolean, Number, or String value at the given key.
     * @param key representing where the value ought to be paired with.
     * @return a String representing the value paired with the key or JsonKey#getValue() if the key isn't present.
     * @throws ClassCastException if the value didn't match the assumed return type.
     * @see com.github.cliftonlabs.json_simple.JsonKey
     *
     * @since 2.3.0 to utilize JsonKey
     */
    fun getStringOrDefault(key: JsonKey): String? {
        var returnable: Any?
        returnable = if (this.containsKey(key.key)) {
            this[key.key]
        } else {
            key.value
        }
        if (returnable is Boolean) {
            returnable = returnable.toString()
        } else if (returnable is Number) {
            returnable = returnable.toString()
        }
        return returnable as String?
    }

    /** Convenience method that calls put for the given key and value.
     * @param key represents the JsonKey used for the value's association in the map.
     * @param value represents the key's association in the map.
     * @see Map.put
     * @since 3.1.1 to use JsonKey instead of calling JsonKey#getKey() each time.
     */
    fun put(key: JsonKey, value: Any?) {
        this[key.key] = value
    }

    /** Calls putAll for the given map, but returns the JsonObject for chaining calls.
     * @param map represents the map to be copied into the JsonObject.
     * @return the JsonObject to allow chaining calls.
     * @see Map.putAll
     * @since 3.1.0 for inline instantiation.
     */
    fun putAllChain(map: Map<String?, Any?>?): JsonObject {
        this.putAll(map!!)
        return this
    }

    /** Convenience method that calls put for the given key and value, but returns the JsonObject for chaining calls.
     * @param key represents the JsonKey used for the value's association in the map.
     * @param value represents the key's association in the map.
     * @return the JsonObject to allow chaining calls.
     * @see Map.put
     * @since 3.1.1 to use JsonKey instead of calling JsonKey#getKey() each time.
     */
    fun putChain(key: JsonKey, value: Any?): JsonObject {
        this[key.key] = value
        return this
    }

    /** Calls put for the given key and value, but returns the JsonObject for chaining calls.
     * @param key represents the value's association in the map.
     * @param value represents the key's association in the map.
     * @return the JsonObject to allow chaining calls.
     * @see Map.put
     * @since 3.1.0 for inline instantiation.
     */
    fun putChain(key: String?, value: Any?): JsonObject {
        this[key] = value
        return this
    }

    /** Convenience method that calls remove for the given key.
     * @param key represents the value's association in the map.
     * @return an object representing the removed value or null if there wasn't one.
     * @since 3.1.1 to use JsonKey instead of calling JsonKey#getKey() each time.
     * @see Map.remove
     */
    fun remove(key: JsonKey): Any? {
        return this.remove(key.key)
    }

    /** Convenience method that calls remove for the given key and value.
     * @param key represents the value's association in the map.
     * @param value represents the expected value at the given key.
     * @return a boolean, which is true if the value was removed. It is false otherwise.
     * @since 3.1.1 to use JsonKey instead of calling JsonKey#getKey() each time.
     * @see Map.remove
     */
    fun remove(key: JsonKey, value: Any?): Boolean {
        return this.remove(key.key, value)
    }

    /** Ensures the given keys are present.
     * @param keys represents the keys that must be present.
     * @throws NoSuchElementException if any of the given keys are missing.
     * @since 2.3.0 to ensure critical keys are in the JsonObject.
     */
    fun requireKeys(vararg keys: JsonKey) {
        /* Track all of the missing keys. */
        val missing: MutableSet<JsonKey> = HashSet()
        for (k in keys) {
            if (!this.containsKey(k.key)) {
                missing.add(k)
            }
        }
        if (!missing.isEmpty()) {
            /* Report any missing keys in the exception. */
            val sb = StringBuilder()
            for (k in missing) {
                sb.append(k.key).append(", ")
            }
            sb.setLength(sb.length - 2)
            val s = if (missing.size > 1) "s" else ""
            throw NoSuchElementException("A JsonObject is missing required key$s: $sb")
        }
    }

    /* (non-Javadoc)
	 * @see org.json.simple.Jsonable#asJsonString() */
    override fun toJson(): String {
        val writable = StringWriter()
        try {
            this.toJson(writable)
        } catch (caught: IOException) {
            /* See java.io.StringWriter. */
        }
        return writable.toString()
    }

    /* (non-Javadoc)
	 * @see org.json.simple.Jsonable#toJsonString(java.io.Writer) */
    @Throws(IOException::class)
    override fun toJson(writable: Writer) {
        /* Writes the map in JSON object format. */
        var isFirstEntry = true
        val entries: Iterator<Map.Entry<String?, Any?>> = entries.iterator()
        writable.write('{'.toInt())
        while (entries.hasNext()) {
            if (isFirstEntry) {
                isFirstEntry = false
            } else {
                writable.write(','.toInt())
            }
            val entry = entries.next()
            Jsoner.serialize(entry.key, writable)
            writable.write(':'.toInt())
            Jsoner.serialize(entry.value, writable)
        }
        writable.write('}'.toInt())
    }

    companion object {
        /** The serialization version this class is compatible with. This value doesn't need to be incremented if and only
         * if the only changes to occur were updating comments, updating javadocs, adding new fields to the class, changing
         * the fields from static to non-static, or changing the fields from transient to non transient. All other changes
         * require this number be incremented.  */
        private const val serialVersionUID = 2L
    }
}