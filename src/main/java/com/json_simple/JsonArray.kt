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
import java.io.IOException
import kotlin.Throws
import java.io.StringWriter
import java.io.Writer

/** JsonArray is a common non-thread safe data format for a collection of data. The contents of a JsonArray are only
 * validated as JSON values on serialization. Meaning all values added to a JsonArray must be recognized by the Jsoner
 * for it to be a true 'JsonArray', so it is really a JsonableArrayList that will serialize to a JsonArray if all of
 * its contents are valid JSON.
 * @see Jsoner
 *
 * @since 2.0.0
 */
class JsonArray : ArrayList<Any?>, Jsonable {
    /** Instantiates an empty JsonArray.  */
    constructor() : super()

    /** Instantiate a new JsonArray using ArrayList's constructor of the same type.
     * @param collection represents the elements to produce the JsonArray with.
     */
    constructor(collection: Collection<*>?) : super(collection)

    /** Calls add for the given collection of elements, but returns the JsonArray for chaining calls.
     * @param collection represents the items to be appended to the JsonArray.
     * @return the JsonArray to allow chaining calls.
     * @see ArrayList.addAll
     * @since 3.1.0 for inline instantiation.
     */
    fun addAllChain(collection: Collection<*>?): JsonArray {
        this.addAll(collection!!)
        return this
    }

    /** Calls add for the given index and collection, but returns the JsonArray for chaining calls.
     * @param index represents what index the element is added to in the JsonArray.
     * @param collection represents the item to be appended to the JsonArray.
     * @return the JsonArray to allow chaining calls.
     * @see ArrayList.addAll
     * @since 3.1.0 for inline instantiation.
     */
    fun addAllChain(index: Int, collection: Collection<*>?): JsonArray {
        this.addAll(index, collection!!)
        return this
    }

    /** Calls add for the given element, but returns the JsonArray for chaining calls.
     * @param index represents what index the element is added to in the JsonArray.
     * @param element represents the item to be appended to the JsonArray.
     * @return the JsonArray to allow chaining calls.
     * @see ArrayList.add
     * @since 3.1.0 for inline instantiation.
     */
    fun addChain(index: Int, element: Any?): JsonArray {
        this.add(index, element)
        return this
    }

    /** Calls add for the given element, but returns the JsonArray for chaining calls.
     * @param element represents the item to be appended to the JsonArray.
     * @return the JsonArray to allow chaining calls.
     * @see ArrayList.add
     * @since 3.1.0 for inline instantiation.
     */
    fun addChain(element: Any?): JsonArray {
        this.add(element)
        return this
    }

    /** A convenience method that assumes every element of the JsonArray is castable to T before adding it to a
     * collection of Ts.
     * @param <T> represents the type that all of the elements of the JsonArray should be cast to and the type the
     * collection will contain.
     * @param destination represents where all of the elements of the JsonArray are added to after being cast to the
     * generic type
     * provided.
     * @throws ClassCastException if the unchecked cast of an element to T fails.
    </T> */
    fun <T> asCollection(destination: MutableCollection<T>) {
        for (o in this) {
            destination.add(o as T)
        }
    }

    /** A convenience method that assumes there is a BigDecimal, Number, or String at the given index. If a Number or
     * String is there it is used to construct a new BigDecimal.
     * @param index representing where the value is expected to be at.
     * @return the value stored at the key or the default provided if the key doesn't exist.
     * @throws ClassCastException if there was a value but didn't match the assumed return types.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal.
     * @see BigDecimal
     *
     * @see Number.toDouble
     */
    fun getBigDecimal(index: Int): BigDecimal? {
        var returnable = this[index]
        if (returnable is BigDecimal) {
            /* Success there was a BigDecimal. */
        } else if (returnable is Number) {
            /* A number can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable.toString())
        } else if (returnable is String) {
            /* A number can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable as String?)
        }
        return returnable as BigDecimal?
    }

    /** A convenience method that assumes there is a Boolean or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a boolean.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     */
    fun getBoolean(index: Int): Boolean? {
        var returnable = this[index]
        if (returnable is String) {
            returnable = java.lang.Boolean.valueOf(returnable as String?)
        }
        return returnable as Boolean?
    }

    /** A convenience method that assumes there is a Number or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a byte.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Number
     */
    fun getByte(index: Int): Byte? {
        var returnable = this[index] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toByte()
    }

    /** A convenience method that assumes there is a Collection value at the given index.
     * @param <T> the kind of collection to expect at the index. Note unless manually added, collection values will be a
     * JsonArray.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a Collection.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Collection
    </T> */
    fun <T : Collection<*>?> getCollection(index: Int): T? {
        /* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
        return this[index] as T?
    }

    /** A convenience method that assumes there is a Number or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a double.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Number
     */
    fun getDouble(index: Int): Double? {
        var returnable = this[index] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toDouble()
    }

    /** A convenience method that assumes there is a Number or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a float.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Number
     */
    fun getFloat(index: Int): Float? {
        var returnable = this[index] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toFloat()
    }

    /** A convenience method that assumes there is a Number or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a int.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Number
     */
    fun getInteger(index: Int): Int? {
        var returnable = this[index] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toInt()
    }

    /** A convenience method that assumes there is a Number or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a long.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Number
     */
    fun getLong(index: Int): Long? {
        var returnable = this[index] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toLong()
    }

    /** A convenience method that assumes there is a Map value at the given index.
     * @param <T> the kind of map to expect at the index. Note unless manually added, Map values will be a JsonObject.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a Map.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Map
    </T> */
    fun <T : Map<*, *>?> getMap(index: Int): T? {
        /* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
        return this[index] as T?
    }

    /** A convenience method that assumes there is a Number or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a short.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws NumberFormatException if a String isn't a valid representation of a BigDecimal or if the Number
     * represents the double or float Infinity or NaN.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     * @see Number
     */
    fun getShort(index: Int): Short? {
        var returnable = this[index] ?: return null
        if (returnable is String) {
            /* A String can be used to construct a BigDecimal. */
            returnable = BigDecimal(returnable)
        }
        return (returnable as Number).toShort()
    }

    /** A convenience method that assumes there is a Boolean, Number, or String value at the given index.
     * @param index represents where the value is expected to be at.
     * @return the value at the index provided cast to a String.
     * @throws ClassCastException if there was a value but didn't match the assumed return type.
     * @throws IndexOutOfBoundsException if the index is outside of the range of element indexes in the JsonArray.
     */
    fun getString(index: Int): String? {
        var returnable = this[index]
        if (returnable is Boolean) {
            returnable = returnable.toString()
        } else if (returnable is Number) {
            returnable = returnable.toString()
        }
        return returnable as String?
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
        var isFirstElement = true
        val elements: Iterator<Any?> = this.iterator()
        writable.write('['.toInt())
        while (elements.hasNext()) {
            if (isFirstElement) {
                isFirstElement = false
            } else {
                writable.write(','.toInt())
            }
            Jsoner.serialize(elements.next(), writable)
        }
        writable.write(']'.toInt())
    }

    companion object {
        /** The serialization version this class is compatible with. This value doesn't need to be incremented if and only
         * if the only changes to occur were updating comments, updating javadocs, adding new fields to the class, changing
         * the fields from static to non-static, or changing the fields from transient to non transient. All other changes
         * require this number be incremented.  */
        private const val serialVersionUID = 1L
    }
}