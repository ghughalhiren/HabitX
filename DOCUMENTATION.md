# Habit Heatmap Tracker - Technical Documentation (v0.3)

## Overview
HabitX is a consistency tracker using a GitHub-style heatmap. Version 0.3 introduces advanced filtering, frequency-based intensity, and improved contextual interactions.

## Features & Requirements (v0.3)

### 1. Interactive Heatmap
- **Passive Indicator**: Tapping a cell displays the specific date and its completion status in a dedicated status bar.
- **Monday Alignment**: The grid always starts weeks on Monday. Day labels (M T W T F S S) are updated accordingly.
- **Filter & Zoom**:
    - Built-in date range picker.
    - Dynamic Zoom: The grid automatically enlarges cells when a shorter range is filtered (e.g., 1-month view shows large, tappable blocks).
- **In-Screen Edit Mode**: Toggle edit mode via the header icon to quickly flip completion states without leaving the dashboard.

### 2. Frequency-Based Logic
- **Weekly Target**: Habits now include a "Weekly Frequency" (3x to Everyday).
- **Color Intensity**: The heatmap applies a color overlay to each weekly column based on performance:
    - **Full Color**: Target met or exceeded.
    - **Faded Color**: 1 or 2 days below target.
    - **Transparent/Grey**: Not started or significantly below target.

### 3. Navigation & Contextual UI
- **Long-Press Actions**: Habit Edit and Delete options are hidden by default. Long-press a habit row to reveal them.
- **Safe Sign Out**: Sign-out is moved to a nested 3-dot overflow menu within the sidebar Account Settings to prevent accidental usage.
- **Dashboard Tap-to-Dismiss**: Tapping any empty area on the dashboard dismisses active contextual actions.

## Data Schema Update
- **Habit Entity**: Added `weeklyFrequency: Int` (Default: 7).
- **Database**: Upgraded to Version 2 with destructive migration support for MVP development.

## Technical Stack
- **Jetpack Compose**: Navigation, Material 3, Adaptive Layouts.
- **Room**: Local persistence with SQL queries.
- **Kotlinx-Datetime**: Date alignment and range calculations.
- **Material Icons Extended**: Improved iconography for filters and actions.
