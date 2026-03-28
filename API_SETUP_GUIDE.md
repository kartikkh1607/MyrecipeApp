# Spoonacular API Setup Guide

## Current Status
🟡 **API is currently DISABLED** - The app is using sample data to avoid API quota issues.

## Why is the API disabled?
The HTTP 401 error you encountered indicates one of these issues:
- API key has exceeded its free quota (150 requests/day)
- API key is invalid or expired
- Network connectivity issues

## How to Enable Spoonacular API

### Step 1: Get a Valid API Key
1. Visit [Spoonacular API](https://spoonacular.com/food-api)
2. Sign up for a free account
3. Get your API key from the dashboard

### Step 2: Update API Key
1. Open `local.properties` file
2. Update the line: `spoonacular.api.key="YOUR_NEW_API_KEY"`
3. Make sure to keep the quotes

### Step 3: Enable API in Code
1. Open `app/src/main/java/com/example/myrecipeapp/MainViewModel.kt`
2. Find the `isSpoonacularApiConfigured()` function
3. Change `return false` to `return true`

```kotlin
private fun isSpoonacularApiConfigured(): Boolean {
    // Set to true when you have a valid Spoonacular API key and sufficient quota
    return true // Change this to true
}
```

### Step 4: Test the API
1. Build and run the app
2. Click on Indian or Italian cuisine
3. You should see recipes loading from the API

## API Quota Management
- **Free Plan**: 150 requests/day
- **Paid Plans**: Higher limits available
- Monitor your usage in the Spoonacular dashboard

## Fallback Strategy
Even with API enabled, the app will automatically fallback to sample data if:
- API requests fail
- Network is unavailable
- API quota is exceeded

## Current Sample Data
With API disabled, the app shows:
- 4 Indian recipes (Butter Chicken, Palak Paneer, Biryani, etc.)
- 4 Italian recipes (Margherita Pizza, Chicken Parmesan, Carbonara, Risotto)
- Other cuisine sample recipes

This ensures users always see relevant recipes even without API access.

## Troubleshooting
1. **401 Error**: Check API key validity and quota
2. **Network Error**: Check internet connection
3. **Empty Results**: API might not have recipes for that cuisine
4. **App Crash**: Check logs for detailed error messages

## Need Help?
- Check Spoonacular API documentation
- Monitor API usage in your dashboard
- Consider upgrading to a paid plan for higher limits
