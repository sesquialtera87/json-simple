package com.json_simple

/** Represents the key of a JsonObject. Utilizing JsonKeys allows many of the convenience methods that self document
 * your JSON data model and make refactoring easier. It is recommended to implement JsonKeys as an enum.
 * @since 2.3.0
 */
interface JsonKey {
    /** The json-simple library uses a String for its keys.
     * @return a String representing the JsonKey.
     */
    val key: String

    /** A reasonable value for the key; such as a valid default, error value, or null.
     * @return an Object representing a reasonable general case value for the key.
     */
    val value: Any
}