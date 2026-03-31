/**
 * ManageAccountsScreen — simple CRUD list for user accounts.
 *
 * Features:
 * - View all accounts
 * - Add a new account (inline form)
 * - Edit an existing account
 * - Archive (soft-delete) an account
 */

import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  TextInput,
  Alert,
  StyleSheet,
} from 'react-native';
import Colors from '../theme/colors';
import {
  getAllAccounts,
  createAccount,
  updateAccount,
  archiveAccount,
} from '../database/AccountRepository';

const ACCOUNT_TYPES = ['Savings', 'Current', 'Credit Card', 'Wallet', 'Loan', 'Other'];

export default function ManageAccountsScreen() {
  const [accounts, setAccounts] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);

  // Form state
  const [name, setName] = useState('');
  const [accountType, setAccountType] = useState('Savings');
  const [currency, setCurrency] = useState('INR');
  const [openingBalance, setOpeningBalance] = useState('0');

  const loadAccounts = useCallback(async () => {
    const data = await getAllAccounts();
    setAccounts(data);
  }, []);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  const resetForm = () => {
    setName('');
    setAccountType('Savings');
    setCurrency('INR');
    setOpeningBalance('0');
    setEditingId(null);
    setShowForm(false);
  };

  const handleSave = async () => {
    if (!name.trim()) {
      Alert.alert('Required', 'Please enter an account name.');
      return;
    }
    const payload = {
      name: name.trim(),
      account_type: accountType,
      currency: currency || 'INR',
      opening_balance: parseFloat(openingBalance) || 0,
    };

    if (editingId) {
      await updateAccount(editingId, payload);
    } else {
      await createAccount(payload);
    }

    resetForm();
    loadAccounts();
  };

  const handleEdit = (account) => {
    setEditingId(account.id);
    setName(account.name || '');
    setAccountType(account.account_type || 'Savings');
    setCurrency(account.currency || 'INR');
    setOpeningBalance(String(account.opening_balance || 0));
    setShowForm(true);
  };

  const handleDelete = (account) => {
    Alert.alert(
      'Archive Account',
      `Archive "${account.name}"? It won't appear in selections anymore.`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Archive',
          style: 'destructive',
          onPress: async () => {
            await archiveAccount(account.id);
            loadAccounts();
          },
        },
      ],
    );
  };

  const renderItem = ({item}) => (
    <View style={styles.card}>
      <View style={styles.cardContent}>
        <Text style={styles.cardName}>{item.name}</Text>
        <Text style={styles.cardSub}>
          {item.account_type} • {item.currency}
        </Text>
      </View>
      <TouchableOpacity style={styles.editBtn} onPress={() => handleEdit(item)}>
        <Text style={styles.editBtnText}>Edit</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.deleteBtn} onPress={() => handleDelete(item)}>
        <Text style={styles.deleteBtnText}>✕</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <View style={styles.container}>
      {/* Account list */}
      <FlatList
        data={accounts}
        renderItem={renderItem}
        keyExtractor={item => item.id}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <Text style={styles.emptyText}>
            No accounts yet. Tap "+" to add one.
          </Text>
        }
      />

      {/* Inline form */}
      {showForm && (
        <View style={styles.formOverlay}>
          <View style={styles.form}>
            <Text style={styles.formTitle}>
              {editingId ? 'Edit Account' : 'New Account'}
            </Text>

            <TextInput
              style={styles.input}
              value={name}
              onChangeText={setName}
              placeholder="Account name"
              placeholderTextColor={Colors.textLight}
            />

            {/* Account type chips */}
            <Text style={styles.fieldLabel}>Account Type</Text>
            <View style={styles.chipRow}>
              {ACCOUNT_TYPES.map(t => (
                <TouchableOpacity
                  key={t}
                  style={[styles.chip, accountType === t && styles.chipActive]}
                  onPress={() => setAccountType(t)}>
                  <Text
                    style={[
                      styles.chipText,
                      accountType === t && styles.chipTextActive,
                    ]}>
                    {t}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <TextInput
              style={styles.input}
              value={currency}
              onChangeText={setCurrency}
              placeholder="Currency (e.g. INR)"
              placeholderTextColor={Colors.textLight}
            />

            <TextInput
              style={styles.input}
              value={openingBalance}
              onChangeText={setOpeningBalance}
              placeholder="Opening balance"
              placeholderTextColor={Colors.textLight}
              keyboardType="numeric"
            />

            <View style={styles.formActions}>
              <TouchableOpacity style={styles.cancelBtn} onPress={resetForm}>
                <Text style={styles.cancelBtnText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.saveBtn} onPress={handleSave}>
                <Text style={styles.saveBtnText}>
                  {editingId ? 'Update' : 'Save'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      )}

      {/* FAB to add */}
      {!showForm && (
        <TouchableOpacity
          style={styles.fab}
          onPress={() => setShowForm(true)}>
          <Text style={styles.fabText}>+</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  list: {
    padding: 16,
    paddingBottom: 80,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 10,
    padding: 14,
    marginBottom: 10,
    elevation: 1,
    shadowColor: Colors.shadow,
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.06,
    shadowRadius: 2,
  },
  cardContent: {
    flex: 1,
  },
  cardName: {
    fontSize: 15,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  cardSub: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  editBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    marginRight: 4,
  },
  editBtnText: {
    color: Colors.primary,
    fontSize: 13,
    fontWeight: '600',
  },
  deleteBtn: {
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  deleteBtnText: {
    color: Colors.promotion,
    fontSize: 16,
    fontWeight: '700',
  },
  emptyText: {
    textAlign: 'center',
    color: Colors.textLight,
    marginTop: 60,
    fontSize: 14,
  },
  formOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'center',
    padding: 20,
  },
  form: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 20,
  },
  formTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 16,
  },
  fieldLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textSecondary,
    marginBottom: 6,
    marginTop: 4,
  },
  input: {
    backgroundColor: Colors.background,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    color: Colors.textPrimary,
    marginBottom: 10,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 10,
    gap: 6,
  },
  chip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    backgroundColor: Colors.chipInactive,
  },
  chipActive: {
    backgroundColor: Colors.primary,
  },
  chipText: {
    fontSize: 13,
    color: Colors.chipTextInactive,
    fontWeight: '500',
  },
  chipTextActive: {
    color: Colors.chipTextActive,
  },
  formActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginTop: 8,
    gap: 10,
  },
  cancelBtn: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
  },
  cancelBtnText: {
    color: Colors.textSecondary,
    fontSize: 15,
    fontWeight: '600',
  },
  saveBtn: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: Colors.primary,
  },
  saveBtnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '600',
  },
  fab: {
    position: 'absolute',
    bottom: 24,
    right: 24,
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 4,
    shadowColor: Colors.shadow,
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  fabText: {
    color: '#FFFFFF',
    fontSize: 28,
    fontWeight: '400',
    lineHeight: 30,
  },
});
