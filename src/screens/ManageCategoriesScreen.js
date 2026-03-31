/**
 * ManageCategoriesScreen — simple CRUD list for transaction categories.
 *
 * Features:
 * - View all categories with icon + color
 * - Add a new category
 * - Edit an existing category
 * - Archive (soft-delete) a category
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
  getAllCategories,
  createCategory,
  updateCategory,
  archiveCategory,
} from '../database/CategoryRepository';

const CATEGORY_TYPES = ['expense', 'income', 'transfer'];

const ICON_OPTIONS = ['🍔', '🚗', '🛒', '💡', '💰', '🔄', '🏥', '🎬', '📦', '🏠', '✈️', '📱', '👕', '🎓', '💳'];

const COLOR_OPTIONS = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
  '#DDA0DD', '#FF8A80', '#B388FF', '#90A4AE', '#FFD93D',
];

export default function ManageCategoriesScreen() {
  const [categories, setCategories] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);

  // Form state
  const [name, setName] = useState('');
  const [type, setType] = useState('expense');
  const [icon, setIcon] = useState('📦');
  const [color, setColor] = useState('#90A4AE');

  const loadCategories = useCallback(async () => {
    const data = await getAllCategories();
    setCategories(data);
  }, []);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  const resetForm = () => {
    setName('');
    setType('expense');
    setIcon('📦');
    setColor('#90A4AE');
    setEditingId(null);
    setShowForm(false);
  };

  const handleSave = async () => {
    if (!name.trim()) {
      Alert.alert('Required', 'Please enter a category name.');
      return;
    }
    const payload = {name: name.trim(), type, icon, color};

    if (editingId) {
      await updateCategory(editingId, payload);
    } else {
      await createCategory(payload);
    }

    resetForm();
    loadCategories();
  };

  const handleEdit = (cat) => {
    setEditingId(cat.id);
    setName(cat.name || '');
    setType(cat.type || 'expense');
    setIcon(cat.icon || '📦');
    setColor(cat.color || '#90A4AE');
    setShowForm(true);
  };

  const handleDelete = (cat) => {
    Alert.alert(
      'Archive Category',
      `Archive "${cat.name}"? It won't appear in selections anymore.`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Archive',
          style: 'destructive',
          onPress: async () => {
            await archiveCategory(cat.id);
            loadCategories();
          },
        },
      ],
    );
  };

  const renderItem = ({item}) => (
    <View style={styles.card}>
      <View style={[styles.iconCircle, {backgroundColor: item.color + '30'}]}>
        <Text style={styles.iconText}>{item.icon}</Text>
      </View>
      <View style={styles.cardContent}>
        <Text style={styles.cardName}>{item.name}</Text>
        <Text style={styles.cardSub}>{item.type}</Text>
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
      <FlatList
        data={categories}
        renderItem={renderItem}
        keyExtractor={item => item.id}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <Text style={styles.emptyText}>
            No categories yet. Tap "+" to add one.
          </Text>
        }
      />

      {/* Inline form */}
      {showForm && (
        <View style={styles.formOverlay}>
          <View style={styles.form}>
            <Text style={styles.formTitle}>
              {editingId ? 'Edit Category' : 'New Category'}
            </Text>

            <TextInput
              style={styles.input}
              value={name}
              onChangeText={setName}
              placeholder="Category name"
              placeholderTextColor={Colors.textLight}
            />

            {/* Type chips */}
            <Text style={styles.fieldLabel}>Type</Text>
            <View style={styles.chipRow}>
              {CATEGORY_TYPES.map(t => (
                <TouchableOpacity
                  key={t}
                  style={[styles.chip, type === t && styles.chipActive]}
                  onPress={() => setType(t)}>
                  <Text
                    style={[
                      styles.chipText,
                      type === t && styles.chipTextActive,
                    ]}>
                    {t.charAt(0).toUpperCase() + t.slice(1)}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Icon picker */}
            <Text style={styles.fieldLabel}>Icon</Text>
            <View style={styles.chipRow}>
              {ICON_OPTIONS.map(i => (
                <TouchableOpacity
                  key={i}
                  style={[styles.iconChip, icon === i && styles.iconChipActive]}
                  onPress={() => setIcon(i)}>
                  <Text style={styles.iconChipText}>{i}</Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Color picker */}
            <Text style={styles.fieldLabel}>Color</Text>
            <View style={styles.chipRow}>
              {COLOR_OPTIONS.map(c => (
                <TouchableOpacity
                  key={c}
                  style={[
                    styles.colorChip,
                    {backgroundColor: c},
                    color === c && styles.colorChipActive,
                  ]}
                  onPress={() => setColor(c)}
                />
              ))}
            </View>

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
  iconCircle: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  iconText: {
    fontSize: 20,
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
    paddingHorizontal: 14,
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
  iconChip: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.chipInactive,
  },
  iconChipActive: {
    backgroundColor: Colors.primaryLight,
    borderWidth: 2,
    borderColor: Colors.primary,
  },
  iconChipText: {
    fontSize: 20,
  },
  colorChip: {
    width: 32,
    height: 32,
    borderRadius: 16,
  },
  colorChipActive: {
    borderWidth: 3,
    borderColor: Colors.textPrimary,
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
