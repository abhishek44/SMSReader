/**
 * FormField — reusable labeled text input for forms.
 *
 * Props:
 *   label: string
 *   value: string
 *   onChangeText: (text) => void
 *   placeholder?: string
 *   keyboardType?: string
 *   multiline?: boolean
 *   editable?: boolean
 */

import React from 'react';
import {View, Text, TextInput, StyleSheet} from 'react-native';
import Colors from '../theme/colors';

export default function FormField({
  label,
  value,
  onChangeText,
  placeholder = '',
  keyboardType = 'default',
  multiline = false,
  editable = true,
}) {
  return (
    <View style={styles.container}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        style={[
          styles.input,
          multiline && styles.multiline,
          !editable && styles.disabled,
        ]}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={Colors.textLight}
        keyboardType={keyboardType}
        multiline={multiline}
        editable={editable}
        numberOfLines={multiline ? 3 : 1}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 14,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textSecondary,
    marginBottom: 6,
  },
  input: {
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    color: Colors.textPrimary,
  },
  multiline: {
    minHeight: 70,
    textAlignVertical: 'top',
  },
  disabled: {
    backgroundColor: Colors.divider,
    color: Colors.textSecondary,
  },
});
