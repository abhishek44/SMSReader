/**
 * EmptyState — shown when there are no messages, permission is denied,
 * or an error occurred.
 *
 * Props:
 *   type: 'loading' | 'permission_denied' | 'empty' | 'fetch_failed'
 *   onRetry: () => void (optional)
 */

import React from 'react';
import {View, Text, TouchableOpacity, StyleSheet, ActivityIndicator} from 'react-native';
import Colors from '../theme/colors';

const STATES = {
  loading: {
    icon: null, // we show an ActivityIndicator instead
    title: 'Loading Messages',
    message: 'Reading your SMS messages...',
    showRetry: false,
  },
  permission_denied: {
    icon: '🔒',
    title: 'Permission Required',
    message:
      'SMS Reader needs permission to read your messages. Please grant SMS access in your device settings.',
    showRetry: true,
    retryLabel: 'Try Again',
  },
  empty: {
    icon: '📭',
    title: 'No Messages',
    message: 'No SMS messages found on this device.',
    showRetry: true,
    retryLabel: 'Refresh',
  },
  fetch_failed: {
    icon: '⚠️',
    title: 'Something Went Wrong',
    message: 'Could not read SMS messages. Please try again.',
    showRetry: true,
    retryLabel: 'Retry',
  },
};

export default function EmptyState({type, onRetry}) {
  const state = STATES[type] || STATES.empty;

  return (
    <View style={styles.container}>
      {type === 'loading' ? (
        <ActivityIndicator size="large" color={Colors.primary} />
      ) : (
        <Text style={styles.icon}>{state.icon}</Text>
      )}

      <Text style={styles.title}>{state.title}</Text>
      <Text style={styles.message}>{state.message}</Text>

      {state.showRetry && onRetry && (
        <TouchableOpacity style={styles.retryButton} onPress={onRetry}>
          <Text style={styles.retryText}>{state.retryLabel}</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  icon: {
    fontSize: 48,
    marginBottom: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 8,
    textAlign: 'center',
  },
  message: {
    fontSize: 14,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: 24,
  },
  retryButton: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 24,
    paddingVertical: 10,
    borderRadius: 20,
  },
  retryText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
});
