package uk.co.scrying

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "technology_nodes")
data class TechnologyNodeEntity(
    @PrimaryKey val id: String,
    val friendlyName: String,
    val category: String,
    val ownership: String,
    val availability: String,
    val capabilities: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val observationCount: Int,
    val rssi: Int?
)

@Dao
interface TechnologyNodeDao {
    @Query("SELECT * FROM technology_nodes ORDER BY lastSeen DESC") fun observeAll(): Flow<List<TechnologyNodeEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(node: TechnologyNodeEntity)
}

@Entity(tableName = "people")
data class PersonEntity(@PrimaryKey val id: String, val displayName: String, val note: String, val createdAt: Long)

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY displayName COLLATE NOCASE") fun observeAll(): Flow<List<PersonEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(person: PersonEntity)
}

@Database(entities = [TechnologyNodeEntity::class, PersonEntity::class], version = 2, exportSchema = true)
abstract class ScryingDatabase : RoomDatabase() { abstract fun nodes(): TechnologyNodeDao; abstract fun people(): PersonDao }

class LocalTechnologyStore(context: Context) {
    private val database = Room.databaseBuilder(context.applicationContext, ScryingDatabase::class.java, "scrying.db").addMigrations(MIGRATION_1_2).build()
    private val dao = database.nodes()
    val nodes: Flow<List<TechnologyNode>> = dao.observeAll().map { list -> list.map(TechnologyNodeEntity::toDomain) }
    suspend fun save(node: TechnologyNode) = dao.upsert(node.toEntity())
    val people: Flow<List<Person>> = database.people().observeAll().map { list -> list.map { Person(it.id, it.displayName, it.note, it.createdAt) } }
    suspend fun save(person: Person) = database.people().upsert(PersonEntity(person.id, person.displayName, person.note, person.createdAt))
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS people (id TEXT NOT NULL, displayName TEXT NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))") }
}

private fun TechnologyNode.toEntity() = TechnologyNodeEntity(id, friendlyName, category.name, ownership.name, availability.name, capabilities.joinToString("\u001F"), firstSeen, lastSeen, observations, rssi)
private fun TechnologyNodeEntity.toDomain() = TechnologyNode(id, friendlyName, TechnologyCategory.valueOf(category), OwnershipState.valueOf(ownership), AvailabilityState.valueOf(availability), capabilities.split("\u001F").filter(String::isNotEmpty).toSet(), firstSeen, lastSeen, observationCount, rssi)
