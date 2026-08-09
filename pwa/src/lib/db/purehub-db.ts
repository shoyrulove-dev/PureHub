import { openDB, type DBSchema, type IDBPDatabase } from 'idb'

export type HabitRecord = {
  id: string
  name: string
  description?: string
  colorHex: string
  targetDaysPerWeek?: number
  category?: string
  createdAt: string
  archivedAt?: string | null
}

export type HabitCheckInRecord = {
  id: string
  habitId: string
  completedOn: string
  note?: string
  createdAt: string
}

export type ExpenseRecord = {
  id: string
  title: string
  amount: number
  category: string
  note?: string
  createdAt: string
}

export type OcrDocumentRecord = {
  id: string
  title: string
  text: string
  source: string
  pageCount: number
  createdAt: string
}

interface PureHubSchema extends DBSchema {
  habits: {
    key: string
    value: HabitRecord
    indexes: {
      'by-created-at': string
    }
  }
  habitCheckIns: {
    key: string
    value: HabitCheckInRecord
    indexes: {
      'by-habit-id': string
      'by-completed-on': string
    }
  }
  expenses: {
    key: string
    value: ExpenseRecord
    indexes: {
      'by-created-at': string
      'by-category': string
    }
  }
  ocrDocuments: {
    key: string
    value: OcrDocumentRecord
    indexes: {
      'by-created-at': string
    }
  }
}

const DB_NAME = 'purehub-offline-db'
const DB_VERSION = 2

let databasePromise: Promise<IDBPDatabase<PureHubSchema>> | null = null

export function getPureHubDb() {
  if (!databasePromise) {
    databasePromise = openDB<PureHubSchema>(DB_NAME, DB_VERSION, {
      upgrade(database, oldVersion) {
        if (oldVersion < 1) {
          const habitStore = database.createObjectStore('habits', { keyPath: 'id' })
          habitStore.createIndex('by-created-at', 'createdAt')

          const checkInStore = database.createObjectStore('habitCheckIns', { keyPath: 'id' })
          checkInStore.createIndex('by-habit-id', 'habitId')
          checkInStore.createIndex('by-completed-on', 'completedOn')

          const expenseStore = database.createObjectStore('expenses', { keyPath: 'id' })
          expenseStore.createIndex('by-created-at', 'createdAt')
          expenseStore.createIndex('by-category', 'category')
        }
        if (oldVersion < 2) {
          const ocrStore = database.createObjectStore('ocrDocuments', { keyPath: 'id' })
          ocrStore.createIndex('by-created-at', 'createdAt')
        }
      },
    })
  }

  return databasePromise
}

export const habitRepository = {
  async list() {
    return (await getPureHubDb()).getAllFromIndex('habits', 'by-created-at')
  },
  async put(record: HabitRecord) {
    return (await getPureHubDb()).put('habits', record)
  },
  async archive(habitId: string, archivedAt: string) {
    const database = await getPureHubDb()
    const current = await database.get('habits', habitId)
    if (!current) return
    await database.put('habits', { ...current, archivedAt })
  },
  async restore(habitId: string) {
    const database = await getPureHubDb()
    const current = await database.get('habits', habitId)
    if (!current) return
    await database.put('habits', { ...current, archivedAt: null })
  },
  async remove(habitId: string) {
    const database = await getPureHubDb()
    const checkIns = await database.getAllFromIndex('habitCheckIns', 'by-habit-id', habitId)
    const transaction = database.transaction(['habits', 'habitCheckIns'], 'readwrite')
    await Promise.all([
      transaction.objectStore('habits').delete(habitId),
      ...checkIns.map((item) => transaction.objectStore('habitCheckIns').delete(item.id)),
      transaction.done,
    ])
  },
}

export const habitCheckInRepository = {
  async listByHabit(habitId: string) {
    return (await getPureHubDb()).getAllFromIndex('habitCheckIns', 'by-habit-id', habitId)
  },
  async upsert(record: HabitCheckInRecord) {
    return (await getPureHubDb()).put('habitCheckIns', record)
  },
  async remove(checkInId: string) {
    return (await getPureHubDb()).delete('habitCheckIns', checkInId)
  },
}

export const expenseRepository = {
  async list() {
    return (await getPureHubDb()).getAllFromIndex('expenses', 'by-created-at')
  },
  async put(record: ExpenseRecord) {
    return (await getPureHubDb()).put('expenses', record)
  },
  async remove(expenseId: string) {
    return (await getPureHubDb()).delete('expenses', expenseId)
  },
}

export const ocrDocumentRepository = {
  async list() {
    const rows = await (await getPureHubDb()).getAllFromIndex('ocrDocuments', 'by-created-at')
    return rows.reverse()
  },
  async put(record: OcrDocumentRecord) {
    return (await getPureHubDb()).put('ocrDocuments', record)
  },
  async remove(id: string) {
    return (await getPureHubDb()).delete('ocrDocuments', id)
  },
  async clear() {
    return (await getPureHubDb()).clear('ocrDocuments')
  },
}
