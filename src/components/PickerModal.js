/**
 * PickerModal — a simple bottom-sheet-style modal to pick from a list.
 *
 * Props:
 *   visible: boolean
 *   title: string
 *   items: Array<{ id, label, sublabel?, icon? }>
 *   selectedId: string | null
 *   onSelect: (item) => void
 *   onClose: () => void
 *   onAdd?: () => void   — optional "Add new" button
 */

import React from 'react';
import {
  View,
  Text,
  Modal,
  TouchableOpacity,
  FlatList,
  StyleSheet,
} from 'react-native';
import Colors from '../theme/colors';

export default function PickerModal({
  visible,
  title,
  items,
  selectedId,
  onSelect,
  onClose,
  onAdd,
}) {
  const renderItem = ({item}) => {
    const isSelected = item.id === selectedId;
    return (
      <TouchableOpacity
        style={[styles.item, isSelected && styles.itemSelected]}
        onPress={() => onSelect(item)}
        activeOpacity={0.6}>
        {item.icon ? (
          <Text style={styles.icon}>{item.icon}</Text>
        ) : null}
        <View style={styles.itemContent}>
          <Text style={[styles.itemLabel, isSelected && styles.itemLabelSelected]}>
            {item.label}
          </Text>
          {item.sublabel ? (
            <Text style={styles.itemSublabel}>{item.sublabel}</Text>
          ) : null}
        </View>
        {isSelected && <Text style={styles.checkmark}>✓</Text>}
      </TouchableOpacity>
    );
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}>
      <TouchableOpacity
        style={styles.backdrop}
        activeOpacity={1}
        onPress={onClose}>
        <View style={styles.sheet}>
          {/* Header */}
          <View style={styles.header}>
            <Text style={styles.title}>{title}</Text>
            <TouchableOpacity onPress={onClose}>
              <Text style={styles.closeBtn}>✕</Text>
            </TouchableOpacity>
          </View>

          {/* List */}
          <FlatList
            data={items}
            renderItem={renderItem}
            keyExtractor={item => item.id}
            style={styles.list}
            ListEmptyComponent={
              <Text style={styles.emptyText}>No items yet</Text>
            }
          />

          {/* Add button */}
          {onAdd && (
            <TouchableOpacity style={styles.addButton} onPress={onAdd}>
              <Text style={styles.addButtonText}>+ Add New</Text>
            </TouchableOpacity>
          )}
        </View>
      </TouchableOpacity>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    maxHeight: '70%',
    paddingBottom: 30,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: Colors.divider,
  },
  title: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  closeBtn: {
    fontSize: 20,
    color: Colors.textSecondary,
    padding: 4,
  },
  list: {
    paddingHorizontal: 12,
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 8,
    borderBottomWidth: 1,
    borderBottomColor: Colors.divider,
  },
  itemSelected: {
    backgroundColor: Colors.primaryLight,
    borderRadius: 8,
  },
  icon: {
    fontSize: 22,
    marginRight: 12,
  },
  itemContent: {
    flex: 1,
  },
  itemLabel: {
    fontSize: 15,
    color: Colors.textPrimary,
    fontWeight: '500',
  },
  itemLabelSelected: {
    color: Colors.primary,
    fontWeight: '700',
  },
  itemSublabel: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  checkmark: {
    fontSize: 18,
    color: Colors.primary,
    fontWeight: '700',
  },
  emptyText: {
    textAlign: 'center',
    color: Colors.textLight,
    paddingVertical: 24,
    fontSize: 14,
  },
  addButton: {
    marginHorizontal: 20,
    marginTop: 8,
    backgroundColor: Colors.primaryLight,
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
  },
  addButtonText: {
    color: Colors.primary,
    fontSize: 15,
    fontWeight: '600',
  },
});
