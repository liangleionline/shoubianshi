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

    /** 下区：全部事项素材库（按使用频率从高到低，再按添加先后） */
    @Query("SELECT * FROM tasks WHERE isPinned = 0 ORDER BY useCount DESC, createdAt ASC")
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

    /** 拖入上区：刷新排序时间，并累计一次使用次数 */
    @Query("UPDATE tasks SET isPinned = 1, createdAt = :now, useCount = useCount + 1 WHERE id = :id")
    suspend fun moveToNow(id: Long, now: Long)

    /** 拖回下区：仅刷新排序时间 */
    @Query("UPDATE tasks SET isPinned = 0, createdAt = :now WHERE id = :id")
    suspend fun moveToPool(id: Long, now: Long)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
