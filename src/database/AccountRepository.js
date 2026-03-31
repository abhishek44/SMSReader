/**
 * AccountRepository — CRUD operations for the accounts table.
 */

import {getDatabase, resultToArray} from './Database';

/**
 * Get all non-archived accounts.
 * @returns {Promise<Array>}
 */
export async function getAllAccounts() {
  const db = await getDatabase();
  const [result] = await db.executeSql(
    'SELECT * FROM accounts WHERE is_archived = 0 ORDER BY name ASC',
  );
  return resultToArray(result);
}

/**
 * Get a single account by ID.
 * @param {string} id
 * @returns {Promise<Object|null>}
 */
export async function getAccountById(id) {
  const db = await getDatabase();
  const [result] = await db.executeSql('SELECT * FROM accounts WHERE id = ?', [id]);
  return result.rows.length > 0 ? result.rows.item(0) : null;
}

/**
 * Create a new account.
 * @param {Object} account - { name, account_type, currency, opening_balance }
 * @returns {Promise<string>} the new account id
 */
export async function createAccount(account) {
  const db = await getDatabase();
  const now = new Date().toISOString();
  const id = 'acc_' + Date.now().toString(36) + '_' + Math.random().toString(36).substr(2, 5);

  await db.executeSql(
    `INSERT INTO accounts (id, name, account_type, currency, opening_balance, is_archived, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, 0, ?, ?)`,
    [
      id,
      account.name || '',
      account.account_type || 'Savings',
      account.currency || 'INR',
      account.opening_balance || 0,
      now,
      now,
    ],
  );

  return id;
}

/**
 * Update an existing account.
 * @param {string} id
 * @param {Object} updates - fields to update
 * @returns {Promise<void>}
 */
export async function updateAccount(id, updates) {
  const db = await getDatabase();
  const now = new Date().toISOString();

  await db.executeSql(
    `UPDATE accounts SET name = ?, account_type = ?, currency = ?, opening_balance = ?, updated_at = ?
     WHERE id = ?`,
    [
      updates.name || '',
      updates.account_type || 'Savings',
      updates.currency || 'INR',
      updates.opening_balance || 0,
      now,
      id,
    ],
  );
}

/**
 * Soft-delete (archive) an account.
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function archiveAccount(id) {
  const db = await getDatabase();
  const now = new Date().toISOString();
  await db.executeSql(
    'UPDATE accounts SET is_archived = 1, updated_at = ? WHERE id = ?',
    [now, id],
  );
}
