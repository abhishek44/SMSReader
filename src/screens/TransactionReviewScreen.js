/**
 * TransactionReviewScreen — review and save a transaction from an SMS.
 *
 * Route params:
 *   message: { id, body, date, category }  — the SMS message
 *   senderAddress: string                   — the sender display name
 *
 * Flow:
 * 1. On mount, parse the SMS text to pre-fill fields
 * 2. User reviews/edits extracted data
 * 3. User picks a Category and Account from modal pickers
 * 4. User optionally adds notes
 * 5. User taps "Save Transaction" → record written to SQLite
 */

import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Alert,
  StyleSheet,
} from 'react-native';
import Colors from '../theme/colors';
import FormField from '../components/FormField';
import PickerModal from '../components/PickerModal';
import {parseTransactionSms} from '../utils/transactionParser';
import {getAllAccounts} from '../database/AccountRepository';
import {getAllCategories} from '../database/CategoryRepository';
import {
  saveTransaction,
  transactionExistsForSms,
} from '../database/TransactionRepository';

export default function TransactionReviewScreen({route, navigation}) {
  const {message, senderAddress} = route.params;

  // ---------- parsed / editable fields ----------
  const [date, setDate] = useState('');
  const [details, setDetails] = useState('');
  const [debit, setDebit] = useState('');
  const [credit, setCredit] = useState('');
  const [accountName, setAccountName] = useState('');
  const [notes, setNotes] = useState('');

  // ---------- selected account ----------
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [showAccountPicker, setShowAccountPicker] = useState(false);

  // ---------- selected category ----------
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [showCategoryPicker, setShowCategoryPicker] = useState(false);

  // ---------- UI state ----------
  const [alreadySaved, setAlreadySaved] = useState(false);
  const [saving, setSaving] = useState(false);

  // Load accounts, categories, and parse SMS on mount
  useEffect(() => {
    (async () => {
      // Check if already saved
      const filename = `sms_${message.id}`;
      const exists = await transactionExistsForSms(filename);
      setAlreadySaved(exists);

      // Load pickers data
      const accs = await getAllAccounts();
      setAccounts(accs);
      const cats = await getAllCategories();
      setCategories(cats);

      // Parse the SMS
      const parsed = parseTransactionSms(message.body, message.date);
      setDate(parsed.date || '');
      setDetails(parsed.details || '');
      setDebit(parsed.debit > 0 ? String(parsed.debit) : '');
      setCredit(parsed.credit > 0 ? String(parsed.credit) : '');
      setAccountName(parsed.account_name || '');

      // Try to auto-match account by type
      if (parsed.account_type && accs.length > 0) {
        const match = accs.find(
          a => a.account_type.toLowerCase() === parsed.account_type.toLowerCase(),
        );
        if (match) {
          setSelectedAccount(match);
        }
      }
    })();
  }, [message]);

  // ---------- handlers ----------

  const handleSelectAccount = useCallback((item) => {
    const acc = accounts.find(a => a.id === item.id);
    setSelectedAccount(acc || null);
    setShowAccountPicker(false);
  }, [accounts]);

  const handleSelectCategory = useCallback((item) => {
    const cat = categories.find(c => c.id === item.id);
    setSelectedCategory(cat || null);
    setShowCategoryPicker(false);
  }, [categories]);

  const handleSave = async () => {
    if (!selectedCategory) {
      Alert.alert('Category Required', 'Please select a category before saving.');
      return;
    }
    if (!selectedAccount) {
      Alert.alert('Account Required', 'Please select an account before saving.');
      return;
    }

    setSaving(true);
    try {
      await saveTransaction({
        date,
        details,
        debit: parseFloat(debit) || 0,
        credit: parseFloat(credit) || 0,
        account_name: selectedAccount.name,
        account_type: selectedAccount.account_type,
        account_id: selectedAccount.id,
        filename: `sms_${message.id}`,
        category: selectedCategory.name,
        category_id: selectedCategory.id,
        notes,
      });

      setAlreadySaved(true);
      Alert.alert('Saved', 'Transaction reviewed and saved successfully.', [
        {text: 'OK', onPress: () => navigation.goBack()},
      ]);
    } catch (err) {
      console.error('Save failed:', err);
      Alert.alert('Error', 'Failed to save the transaction. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // ---------- picker data formatting ----------

  const accountPickerItems = accounts.map(a => ({
    id: a.id,
    label: a.name,
    sublabel: `${a.account_type} • ${a.currency}`,
  }));

  const categoryPickerItems = categories.map(c => ({
    id: c.id,
    label: c.name,
    sublabel: c.type,
    icon: c.icon,
  }));

  // ---------- render ----------

  return (
    <View style={styles.container}>
      <ScrollView
        contentContainerStyle={styles.scroll}
        showsVerticalScrollIndicator={false}>

        {/* Already saved banner */}
        {alreadySaved && (
          <View style={styles.savedBanner}>
            <Text style={styles.savedBannerText}>
              ✓ This transaction has already been reviewed and saved.
            </Text>
          </View>
        )}

        {/* Original SMS preview */}
        <View style={styles.smsCard}>
          <Text style={styles.smsLabel}>Original SMS</Text>
          <Text style={styles.smsSender}>{senderAddress}</Text>
          <Text style={styles.smsBody}>{message.body}</Text>
        </View>

        {/* Parsed fields */}
        <Text style={styles.sectionTitle}>Transaction Details</Text>

        <FormField
          label="Date"
          value={date}
          onChangeText={setDate}
          placeholder="YYYY-MM-DD"
        />

        <FormField
          label="Details / Description"
          value={details}
          onChangeText={setDetails}
          placeholder="Merchant, reference, etc."
          multiline
        />

        <View style={styles.row}>
          <View style={styles.halfField}>
            <FormField
              label="Debit Amount"
              value={debit}
              onChangeText={setDebit}
              placeholder="0.00"
              keyboardType="numeric"
            />
          </View>
          <View style={styles.halfField}>
            <FormField
              label="Credit Amount"
              value={credit}
              onChangeText={setCredit}
              placeholder="0.00"
              keyboardType="numeric"
            />
          </View>
        </View>

        <FormField
          label="Account Number / Name (from SMS)"
          value={accountName}
          onChangeText={setAccountName}
          placeholder="Auto-detected from SMS"
        />

        {/* Account picker */}
        <Text style={styles.sectionTitle}>Account</Text>
        <TouchableOpacity
          style={styles.pickerButton}
          onPress={() => setShowAccountPicker(true)}>
          {selectedAccount ? (
            <View>
              <Text style={styles.pickerValue}>{selectedAccount.name}</Text>
              <Text style={styles.pickerSub}>
                {selectedAccount.account_type} • {selectedAccount.currency}
              </Text>
            </View>
          ) : (
            <Text style={styles.pickerPlaceholder}>Tap to select account</Text>
          )}
          <Text style={styles.pickerArrow}>›</Text>
        </TouchableOpacity>

        {/* Category picker */}
        <Text style={styles.sectionTitle}>Category</Text>
        <TouchableOpacity
          style={styles.pickerButton}
          onPress={() => setShowCategoryPicker(true)}>
          {selectedCategory ? (
            <View style={styles.pickerRow}>
              <Text style={styles.pickerIcon}>{selectedCategory.icon}</Text>
              <View>
                <Text style={styles.pickerValue}>{selectedCategory.name}</Text>
                <Text style={styles.pickerSub}>{selectedCategory.type}</Text>
              </View>
            </View>
          ) : (
            <Text style={styles.pickerPlaceholder}>Tap to select category</Text>
          )}
          <Text style={styles.pickerArrow}>›</Text>
        </TouchableOpacity>

        {/* Notes */}
        <Text style={styles.sectionTitle}>Notes (optional)</Text>
        <FormField
          label=""
          value={notes}
          onChangeText={setNotes}
          placeholder="Add a note about this transaction..."
          multiline
        />

        {/* Save button */}
        <TouchableOpacity
          style={[styles.saveButton, (saving || alreadySaved) && styles.saveButtonDisabled]}
          onPress={handleSave}
          disabled={saving || alreadySaved}>
          <Text style={styles.saveButtonText}>
            {saving ? 'Saving...' : alreadySaved ? 'Already Saved' : 'Save Transaction'}
          </Text>
        </TouchableOpacity>

        <View style={styles.bottomSpacer} />
      </ScrollView>

      {/* Account picker modal */}
      <PickerModal
        visible={showAccountPicker}
        title="Select Account"
        items={accountPickerItems}
        selectedId={selectedAccount?.id}
        onSelect={handleSelectAccount}
        onClose={() => setShowAccountPicker(false)}
        onAdd={() => {
          setShowAccountPicker(false);
          navigation.navigate('ManageAccounts');
        }}
      />

      {/* Category picker modal */}
      <PickerModal
        visible={showCategoryPicker}
        title="Select Category"
        items={categoryPickerItems}
        selectedId={selectedCategory?.id}
        onSelect={handleSelectCategory}
        onClose={() => setShowCategoryPicker(false)}
        onAdd={() => {
          setShowCategoryPicker(false);
          navigation.navigate('ManageCategories');
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  scroll: {
    padding: 16,
  },
  savedBanner: {
    backgroundColor: '#D1FAE5',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  savedBannerText: {
    color: '#065F46',
    fontSize: 13,
    fontWeight: '600',
  },
  smsCard: {
    backgroundColor: Colors.surface,
    borderRadius: 10,
    padding: 14,
    marginBottom: 20,
    borderLeftWidth: 4,
    borderLeftColor: Colors.transaction,
  },
  smsLabel: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textLight,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  smsSender: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textPrimary,
    marginBottom: 6,
  },
  smsBody: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 19,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 10,
    marginTop: 6,
  },
  row: {
    flexDirection: 'row',
    gap: 12,
  },
  halfField: {
    flex: 1,
  },
  pickerButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 16,
  },
  pickerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  pickerIcon: {
    fontSize: 22,
    marginRight: 10,
  },
  pickerValue: {
    fontSize: 15,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  pickerSub: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 1,
  },
  pickerPlaceholder: {
    flex: 1,
    fontSize: 15,
    color: Colors.textLight,
  },
  pickerArrow: {
    fontSize: 22,
    color: Colors.textLight,
    marginLeft: 'auto',
    paddingLeft: 8,
  },
  saveButton: {
    backgroundColor: Colors.primary,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 10,
  },
  saveButtonDisabled: {
    backgroundColor: Colors.border,
  },
  saveButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  bottomSpacer: {
    height: 30,
  },
});
