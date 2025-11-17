# API Setup Guide

## Overview

RotDex requires an AI image generation API to create unique brainrot cards. This guide covers setup for supported providers.

## Supported AI Providers

### 1. OpenAI DALL-E
- **Best For**: High-quality, detailed images
- **Cost**: Pay-per-generation
- **Setup Difficulty**: Easy

### 2. Stability AI (Stable Diffusion)
- **Best For**: Customizable, open-source
- **Cost**: Free tier available
- **Setup Difficulty**: Medium

### 3. Replicate
- **Best For**: Multiple models, easy integration
- **Cost**: Pay-as-you-go
- **Setup Difficulty**: Easy

### 4. Local Models (Future)
- **Best For**: Privacy, no API costs
- **Cost**: Free (requires powerful device)
- **Setup Difficulty**: Advanced

## Quick Start with OpenAI

### Step 1: Get API Key

1. Visit [OpenAI Platform](https://platform.openai.com/)
2. Create an account or sign in
3. Navigate to API Keys section
4. Create a new API key
5. Copy the key (you won't see it again!)

### Step 2: Configure the App

1. Create `local.properties` in the project root:

```properties
# API Keys (DO NOT commit this file!)
OPENAI_API_KEY=sk-your-api-key-here
```

2. Add to `.gitignore` (already included):
```
**/api_keys.properties
**/secrets.properties
local.properties
```

### Step 3: Update Build Configuration

The app will automatically read from `local.properties` and inject the API key securely.

## Alternative: Stability AI Setup

### Step 1: Get API Key

1. Visit [Stability AI](https://platform.stability.ai/)
2. Create account and verify email
3. Navigate to API Keys
4. Generate new key

### Step 2: Configure

Add to `local.properties`:
```properties
STABILITY_API_KEY=sk-your-stability-key-here
```

### Step 3: Select Provider

In the app settings, choose Stability AI as your provider.

## API Configuration

### Model Selection

Configure which AI model to use in `ApiConfig.kt`:

```kotlin
object ApiConfig {
    const val PROVIDER = "openai" // or "stability", "replicate"
    const val MODEL = "dall-e-3" // or "stable-diffusion-xl"
    const val IMAGE_SIZE = "1024x1024"
    const val QUALITY = "standard" // or "hd"
}
```

### Rate Limiting

To avoid API costs:

```kotlin
object RateLimits {
    const val MAX_GENERATIONS_PER_DAY = 50
    const val MAX_CONCURRENT_REQUESTS = 3
    const val RETRY_ATTEMPTS = 3
}
```

## Cost Optimization

### Tips to Reduce API Costs:

1. **Cache Generated Images**: Store URLs locally
2. **Batch Requests**: Generate multiple variations at once
3. **Use Lower Quality**: Start with standard quality
4. **Set Daily Limits**: Prevent accidental overspending
5. **Monitor Usage**: Track API calls in your dashboard

### Estimated Costs (OpenAI DALL-E 3):

- Standard: ~$0.04 per image
- HD Quality: ~$0.08 per image
- 100 cards/month: ~$4-8

## Error Handling

The app handles common API errors:

- **Rate Limit**: Shows cooldown timer
- **Invalid Key**: Prompts for configuration
- **Network Error**: Retry with exponential backoff
- **Quota Exceeded**: Notifies user to upgrade plan

## Security Best Practices

1. ✅ **Never commit API keys**
2. ✅ **Use environment variables or local.properties**
3. ✅ **Rotate keys periodically**
4. ✅ **Monitor usage dashboards**
5. ✅ **Set spending limits on provider platforms**
6. ✅ **Use ProGuard for release builds**

## Testing Without API Key

For development without API costs:

1. **Mock Mode**: Use placeholder images
2. **Local Assets**: Pre-generated sample cards
3. **Test API Keys**: Some providers offer test credentials

Enable mock mode in `local.properties`:
```properties
USE_MOCK_API=true
```

## Troubleshooting

### "Invalid API Key" Error
- Double-check key in `local.properties`
- Ensure no extra spaces or quotes
- Verify key is active in provider dashboard

### "Rate Limit Exceeded"
- Wait for cooldown period
- Upgrade API plan
- Reduce generation frequency

### "Network Error"
- Check internet connection
- Verify API endpoint URL
- Check provider status page

### "Quota Exceeded"
- Add payment method to provider account
- Upgrade to paid tier
- Wait for monthly reset (free tiers)

## Advanced Configuration

### Custom Endpoints

For self-hosted models:
```properties
CUSTOM_API_ENDPOINT=http://your-server.com/generate
CUSTOM_API_KEY=your-key
```

### Prompt Engineering

Optimize prompts in `PromptBuilder.kt`:
```kotlin
fun buildPrompt(userInput: String): String {
    return """
        Create a trading card style image featuring: $userInput
        Style: vibrant, digital art, meme culture
        Format: vertical card with clear focal point
    """.trimIndent()
}
```

## Support

- **OpenAI Docs**: https://platform.openai.com/docs
- **Stability AI Docs**: https://platform.stability.ai/docs
- **App Issues**: GitHub Issues

---

**Remember**: Keep your API keys secure and never share them publicly!
