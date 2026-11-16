package com.example.mpesatracker.data

import androidx.room.TypeConverter
import com.example.mpesatracker.domain.Category
import com.example.mpesatracker.domain.Source
import com.example.mpesatracker.domain.TransactionType

/**
 * SQLite only stores numbers, text and blobs, so Room uses these functions
 * to save our enums as their names (e.g. "FOOD") and read them back.
 */
class Converters {
    @TypeConverter fun categoryToText(value: Category): String = value.name
    @TypeConverter fun textToCategory(value: String): Category = Category.valueOf(value)

    @TypeConverter fun typeToText(value: TransactionType): String = value.name
    @TypeConverter fun textToType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter fun sourceToText(value: Source): String = value.name
    @TypeConverter fun textToSource(value: String): Source = Source.valueOf(value)
}
