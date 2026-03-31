/**
 * AppNavigator — Stack navigator for the app.
 *
 * Screens:
 *   Home              — conversation list with category tabs
 *   ThreadDetail      — messages in a selected conversation
 *   TransactionReview — review & save a transaction SMS
 *   ManageAccounts    — CRUD for user accounts
 *   ManageCategories  — CRUD for transaction categories
 */

import React from 'react';
import {NavigationContainer} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import Colors from '../theme/colors';
import HomeScreen from '../screens/HomeScreen';
import ThreadDetailScreen from '../screens/ThreadDetailScreen';
import TransactionReviewScreen from '../screens/TransactionReviewScreen';
import ManageAccountsScreen from '../screens/ManageAccountsScreen';
import ManageCategoriesScreen from '../screens/ManageCategoriesScreen';

const Stack = createNativeStackNavigator();

export default function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName="Home"
        screenOptions={{
          headerStyle: {
            backgroundColor: Colors.surface,
          },
          headerTintColor: Colors.textPrimary,
          headerTitleStyle: {
            fontWeight: '700',
            fontSize: 18,
          },
          headerShadowVisible: false,
          contentStyle: {
            backgroundColor: Colors.background,
          },
        }}>
        <Stack.Screen
          name="Home"
          component={HomeScreen}
          options={{
            title: 'SMS Reader',
          }}
        />
        <Stack.Screen
          name="ThreadDetail"
          component={ThreadDetailScreen}
          options={({route}) => ({
            title: route.params.displayAddress || 'Conversation',
          })}
        />
        <Stack.Screen
          name="TransactionReview"
          component={TransactionReviewScreen}
          options={{
            title: 'Review Transaction',
          }}
        />
        <Stack.Screen
          name="ManageAccounts"
          component={ManageAccountsScreen}
          options={{
            title: 'Manage Accounts',
          }}
        />
        <Stack.Screen
          name="ManageCategories"
          component={ManageCategoriesScreen}
          options={{
            title: 'Manage Categories',
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
