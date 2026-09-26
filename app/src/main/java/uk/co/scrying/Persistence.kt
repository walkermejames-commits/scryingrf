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

@Database(entities = [TechnologyNodeEntity::class], version = 1, exportSchema = true)
abstract class ScryingDatabase : RoomDatabase() { abstract fun nodes(): TechnologyNodeDao }

class LocalTechnologyStore(context: Context) {
    private val dao = Room.databaseBuilder(context.applicationContext, ScryingDatabase::class.java, "scrying.db").build().nodes()
    val nodes: Flow<List<TechnologyNode>> = dao.observeAll().map { list -> list.map(TechnologyNodeEntity::toDomain) }
    suspend fun save(node: TechnologyNode) = dao.upsert(node.toEntity())
}

private fun TechnologyNode.toEntity() = TechnologyNodeEntity(id, friendlyName, category.name, ownership.name, availability.name, capabilities.joinToString("\u001F"), firstSeen, lastSeen, observations, rssi)
private fun TechnologyNodeEntity.toDomain() = TechnologyNode(id, friendlyName, TechnologyCategory.valueOf(category), OwnershipState.valueOf(ownership), AvailabilityState.valueOf(availability), capabilities.split("\u001F").filter(String::isNotEmpty).toSet(), firstSeen, lastSeen, observationCount, rssi)
