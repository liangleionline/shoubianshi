package com.liangleionline.shoubianshi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /** 上区：接下来待办 */
    @Query("SELECT * FROM tasks WHERE isPinned = 1 ORDER BY createdAt ASC")
    fun observeNowTasks(): Flow<List<Task>>

    /** 下区：全部事项素材库 */
    @Query("SELECT * FROM tasks WHERE isPinned = 0 ORDER BY createdAt ASC")
    fun observePoolTasks(): Flow<List<Task>>

    /** 管理页：全部事项 */
    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, createdAt ASC")
    fun observeAllTasks(): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("UPDATE tasks SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    /** 移动到目标区时同时刷新排序时间，使该胶囊排到末尾 */
    @Query("UPDATE tasks SET isPinned = :pinned, createdAt = :now WHERE id = :id")
    suspend fun setPinnedAt(id: Long, pinned: Boolean, now: Long)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
