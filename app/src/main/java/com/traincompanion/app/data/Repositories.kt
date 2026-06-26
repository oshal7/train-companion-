package com.traincompanion.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DocumentsRepository(private val store: SecureFileStore) {

    private val tableName = "documents.json"

    suspend fun list(): List<StoredDocument> = withContext(Dispatchers.IO) {
        readAll()
    }

    suspend fun add(type: DocumentType, displayName: String, mimeType: String, bytes: ByteArray): StoredDocument =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val extension = mimeType.substringAfterLast('/', "bin")
            val fileName = "$id.$extension.enc"
            store.writeDocumentBytes(fileName, bytes)
            val doc = StoredDocument(
                id = id,
                type = type,
                displayName = displayName,
                fileName = fileName,
                mimeType = mimeType,
                addedAtMillis = System.currentTimeMillis()
            )
            val all = readAll() + doc
            writeAll(all)
            doc
        }

    suspend fun readBytes(document: StoredDocument): ByteArray = withContext(Dispatchers.IO) {
        store.readDocumentBytes(document.fileName)
    }

    suspend fun delete(document: StoredDocument) = withContext(Dispatchers.IO) {
        store.deleteDocument(document.fileName)
        writeAll(readAll().filterNot { it.id == document.id })
    }

    private fun readAll(): List<StoredDocument> {
        val raw = store.readJson(tableName) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            StoredDocument(
                id = o.getString("id"),
                type = DocumentType.valueOf(o.getString("type")),
                displayName = o.getString("displayName"),
                fileName = o.getString("fileName"),
                mimeType = o.getString("mimeType"),
                addedAtMillis = o.getLong("addedAtMillis")
            )
        }
    }

    private fun writeAll(docs: List<StoredDocument>) {
        val arr = JSONArray()
        docs.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("type", d.type.name)
                    .put("displayName", d.displayName)
                    .put("fileName", d.fileName)
                    .put("mimeType", d.mimeType)
                    .put("addedAtMillis", d.addedAtMillis)
            )
        }
        store.writeJson(tableName, arr.toString())
    }
}

class ContactsRepository(private val store: SecureFileStore) {

    private val tableName = "contacts.json"

    suspend fun list(): List<EmergencyContact> = withContext(Dispatchers.IO) { readAll() }

    suspend fun add(name: String, phone: String): EmergencyContact = withContext(Dispatchers.IO) {
        val contact = EmergencyContact(UUID.randomUUID().toString(), name, phone)
        writeAll(readAll() + contact)
        contact
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        writeAll(readAll().filterNot { it.id == id })
    }

    private fun readAll(): List<EmergencyContact> {
        val raw = store.readJson(tableName) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            EmergencyContact(o.getString("id"), o.getString("name"), o.getString("phone"))
        }
    }

    private fun writeAll(contacts: List<EmergencyContact>) {
        val arr = JSONArray()
        contacts.forEach { c ->
            arr.put(JSONObject().put("id", c.id).put("name", c.name).put("phone", c.phone))
        }
        store.writeJson(tableName, arr.toString())
    }
}

class AlarmsRepository(private val store: SecureFileStore) {

    private val tableName = "alarms.json"

    suspend fun list(): List<ScheduledAlarm> = withContext(Dispatchers.IO) { readAll() }

    suspend fun upsert(alarm: ScheduledAlarm) = withContext(Dispatchers.IO) {
        val all = readAll().filterNot { it.id == alarm.id } + alarm
        writeAll(all)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        writeAll(readAll().filterNot { it.id == id })
    }

    private fun readAll(): List<ScheduledAlarm> {
        val raw = store.readJson(tableName) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ScheduledAlarm(
                id = o.getString("id"),
                label = o.getString("label"),
                triggerAtMillis = o.getLong("triggerAtMillis"),
                bufferMinutes = o.optInt("bufferMinutes", 20),
                sourceTrainNumber = o.optString("sourceTrainNumber").takeIf { it.isNotBlank() },
                enabled = o.optBoolean("enabled", true)
            )
        }
    }

    private fun writeAll(alarms: List<ScheduledAlarm>) {
        val arr = JSONArray()
        alarms.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("label", a.label)
                    .put("triggerAtMillis", a.triggerAtMillis)
                    .put("bufferMinutes", a.bufferMinutes)
                    .put("sourceTrainNumber", a.sourceTrainNumber ?: "")
                    .put("enabled", a.enabled)
            )
        }
        store.writeJson(tableName, arr.toString())
    }
}

class PnrRepository(private val store: SecureFileStore) {

    private val tableName = "pnr_records.json"

    suspend fun list(): List<PnrRecord> = withContext(Dispatchers.IO) { readAll() }

    suspend fun save(record: PnrRecord) = withContext(Dispatchers.IO) {
        val all = readAll().filterNot { it.pnr == record.pnr } + record
        writeAll(all)
    }

    private fun readAll(): List<PnrRecord> {
        val raw = store.readJson(tableName) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PnrRecord(
                pnr = o.getString("pnr"),
                status = o.getString("status"),
                coach = o.getString("coach"),
                berth = o.getString("berth"),
                travelClass = o.getString("travelClass"),
                trainNumber = o.getString("trainNumber"),
                trainName = o.getString("trainName"),
                lastCheckedMillis = o.getLong("lastCheckedMillis")
            )
        }
    }

    private fun writeAll(records: List<PnrRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject()
                    .put("pnr", r.pnr)
                    .put("status", r.status)
                    .put("coach", r.coach)
                    .put("berth", r.berth)
                    .put("travelClass", r.travelClass)
                    .put("trainNumber", r.trainNumber)
                    .put("trainName", r.trainName)
                    .put("lastCheckedMillis", r.lastCheckedMillis)
            )
        }
        store.writeJson(tableName, arr.toString())
    }
}
