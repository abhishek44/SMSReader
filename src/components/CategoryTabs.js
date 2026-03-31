/**
 * CategoryTabs — horizontal filter chips for switching message categories.
 *
 * Props:
 *   categories: [{key, label, count}]
 *   activeCategory: string
 *   onPress: (categoryKey) => void
 */

import React from 'react';
import {View, Text, TouchableOpacity, StyleSheet, ScrollView} from 'react-native';
import Colors from '../theme/colors';

const CATEGORY_COLORS = {
  all: Colors.all,
  transaction: Colors.transaction,
  personal: Colors.personal,
  promotion: Colors.promotion,
};

export default function CategoryTabs({categories, activeCategory, onPress}) {
  return (
    <View style={styles.container}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}>
        {categories.map(cat => {
          const isActive = cat.key === activeCategory;
          const activeColor = CATEGORY_COLORS[cat.key] || Colors.primary;

          return (
            <TouchableOpacity
              key={cat.key}
              style={[
                styles.chip,
                isActive && {backgroundColor: activeColor},
              ]}
              onPress={() => onPress(cat.key)}
              activeOpacity={0.7}>
              <Text
                style={[
                  styles.chipText,
                  isActive && styles.chipTextActive,
                ]}>
                {cat.label}
              </Text>
              {cat.count !== undefined && (
                <View
                  style={[
                    styles.badge,
                    isActive && {backgroundColor: 'rgba(255,255,255,0.3)'},
                  ]}>
                  <Text
                    style={[
                      styles.badgeText,
                      isActive && styles.badgeTextActive,
                    ]}>
                    {cat.count}
                  </Text>
                </View>
              )}
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.surface,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  scrollContent: {
    paddingHorizontal: 16,
    gap: 8,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: Colors.chipInactive,
    marginRight: 8,
  },
  chipText: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.chipTextInactive,
  },
  chipTextActive: {
    color: Colors.chipTextActive,
  },
  badge: {
    marginLeft: 6,
    backgroundColor: Colors.border,
    borderRadius: 10,
    paddingHorizontal: 6,
    paddingVertical: 1,
    minWidth: 20,
    alignItems: 'center',
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  badgeTextActive: {
    color: Colors.chipTextActive,
  },
});
