/**
 * Database — central SQLite helper.
 *
 * Opens (or creates) the local database and provides
 * table creation for: transactions, accounts, categories.
 *
 * Uses react-native-sqlite-storage.
 */

import SQLite from 'react-native-sqlite-storage';

// Enable promise-based API
SQLite.enablePromise(true);

const DB_NAME = 'smsreader.db';

let dbInstance = null;

/**
 * Get (or open) the database singleton.
 * @returns {Promise<SQLite.SQLiteDatabase>}
 */
export async function getDatabase() {
  if (dbInstance) {
    return dbInstance;
  }

  dbInstance = await SQLite.openDatabase({
    name: DB_NAME,
    location: 'default',
  });

  await createTables(dbInstance);
  return dbInstance;
}

/**
 * Create all tables if they don't exist.
 */
async function createTables(db) {
  await db.executeSql(`
    CREATE TABLE IF NOT EXISTS accounts (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      account_type TEXT,
      currency TEXT DEFAULT 'INR',
      opening_balance REAL DEFAULT 0,
      is_archived INTEGER DEFAULT 0,
      created_at TEXT,
      updated_at TEXT
    );
  `);

  await db.executeSql(`
    CREATE TABLE IF NOT EXISTS categories (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      type TEXT,
      icon TEXT,
      color TEXT,
      is_archived INTEGER DEFAULT 0,
      created_at TEXT,
      updated_at TEXT
    );
  `);

  await db.executeSql(`
    CREATE TABLE IF NOT EXISTS transactions (
      Id INTEGER PRIMARY KEY AUTOINCREMENT,
      Date TEXT,
      Details TEXT,
      Debit REAL DEFAULT 0,
      Credit REAL DEFAULT 0,
      Account_name TEXT,
      Account_type TEXT,
      account_id TEXT,
      filename TEXT,
      review_status TEXT DEFAULT 'pending',
      review_datetime TEXT,
      uploaded_datetime TEXT,
      Category TEXT,
      category_id TEXT,
      Notes TEXT,
      FOREIGN KEY (account_id) REFERENCES accounts(id),
      FOREIGN KEY (category_id) REFERENCES categories(id)
    );
  `);

  // Seed some default categories if empty
  const [catResult] = await db.executeSql('SELECT COUNT(*) as cnt FROM categories');
  if (catResult.rows.item(0).cnt === 0) {
    const now = new Date().toISOString();
    const defaults = [
      {id: 'cat_food', name: 'Food & Dining', type: 'expense', icon: '🍔', color: '#FF6B6B'},
      {id: 'cat_transport', name: 'Transport', type: 'expense', icon: '🚗', color: '#4ECDC4'},
      {id: 'cat_shopping', name: 'Shopping', type: 'expense', icon: '🛒', color: '#45B7D1'},
      {id: 'cat_bills', name: 'Bills & Utilities', type: 'expense', icon: '💡', color: '#96CEB4'},
      {id: 'cat_salary', name: 'Salary', type: 'income', icon: '💰', color: '#FFEAA7'},
      {id: 'cat_transfer', name: 'Transfer', type: 'transfer', icon: '🔄', color: '#DDA0DD'},
      {id: 'cat_health', name: 'Health', type: 'expense', icon: '🏥', color: '#FF8A80'},
      {id: 'cat_entertainment', name: 'Entertainment', type: 'expense', icon: '🎬', color: '#B388FF'},
      {id: 'cat_other', name: 'Other', type: 'expense', icon: '📦', color: '#90A4AE'},
    ];
    for (const cat of defaults) {
      await db.executeSql(
        'INSERT INTO categories (id, name, type, icon, color, is_archived, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0, ?, ?)',
        [cat.id, cat.name, cat.type, cat.icon, cat.color, now, now],
      );
    }
  }

  // Seed some default accounts if empty
  const [accResult] = await db.executeSql('SELECT COUNT(*) as cnt FROM accounts');
  if (accResult.rows.item(0).cnt === 0) {
    const now = new Date().toISOString();
    const defaults = [
      {id: 'acc_savings', name: 'Savings Account', account_type: 'Savings', currency: 'INR'},
      {id: 'acc_credit', name: 'Credit Card', account_type: 'Credit Card', currency: 'INR'},
      {id: 'acc_wallet', name: 'Wallet', account_type: 'Wallet', currency: 'INR'},
    ];
    for (const acc of defaults) {
      await db.executeSql(
        'INSERT INTO accounts (id, name, account_type, currency, opening_balance, is_archived, created_at, updated_at) VALUES (?, ?, ?, ?, 0, 0, ?, ?)',
        [acc.id, acc.name, acc.account_type, acc.currency, now, now],
      );
    }
  }
}

/**
 * Helper: convert SQLite result rows to a plain JS array.
 * @param {SQLite.ResultSet} result
 * @returns {Array<Object>}
 */
export function resultToArray(result) {
  const rows = [];
  for (let i = 0; i < result.rows.length; i++) {
    rows.push(result.rows.item(i));
  }
  return rows;
}

/**
 * Close the database (call on app unmount if needed).
 */
export async function closeDatabase() {
  if (dbInstance) {
    await dbInstance.close();
    dbInstance = null;
  }
}
