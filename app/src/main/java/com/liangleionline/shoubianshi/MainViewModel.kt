package com.liangleionline.shoubianshi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liangleionline.shoubianshi.data.AppDatabase
import com.liangleionline.shoubianshi.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).taskDao()

    /** 上区：接下来待办 */
    val nowTasks: Flow<List<Task>> = dao.observeNowTasks()

    /** 下区：全部事项素材库 */
    val poolTasks: Flow<List<Task>> = dao.observePoolTasks()

    /** 管理页：全部事项 */
    val allTasks: Flow<List<Task>> = dao.observeAllTasks()

    /** 新增事项（默认进入素材库） */
    fun addTask(title: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.insert(Task(title = title, isPinned = false))
    }

    /** 编辑事项标题 */
    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        dao.update(task)
    }

    /** 把事项拖入上区（加入待办），并排到末尾 */
    fun moveToNow(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        dao.moveToNow(id, now = System.currentTimeMillis())
    }

    /** 把事项拖回下区（取消待办），并排到末尾 */
    fun moveToPool(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        dao.moveToPool(id, now = System.currentTimeMillis())
    }

    /** 完成并删除 */
    fun delete(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        dao.delete(task)
    }

    /** 拖入完成区：按 id 完成并删除 */
    fun deleteById(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteById(id)
    }
}
