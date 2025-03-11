# Expense Tracking Automation

## Overview
This project automates expense tracking by processing bank SMS notifications without requiring manual input or exposing SMS data to third-party apps. The system currently works on **iOS** using built-in automation.

## Features
- **Automated Expense Tracking**: No manual entry required.
- **Secure SMS Processing**: Data is processed privately.
- **Analytics Dashboard**: Categorize and analyze spending.
- **Budget Alerts**: Get notified when exceeding spending limits.

## How It Works
1. **iOS Automation** triggers when a bank SMS is received and sends it to a **REST endpoint**.
2. **AWS Lambda** processes the request and calls **OpenAI** to extract meaningful details.
3. Extracted data is stored in **DynamoDB**.
4. The data can be used for analytics and alerts.

## Prerequisites
- iPhone with **iOS Automation (Shortcuts App)**
- AWS account with:
  - API Gateway
  - Lambda Function
  - DynamoDB
- OpenAI API key (for SMS parsing)

## Setup
1. Clone this repository:
   ```sh
   git clone https://github.com/your-username/expense-tracker.git
   cd expense-tracker
   ```
2. Set up your AWS Lambda function and API Gateway.
3. Configure iOS Automation to send SMS content to your API endpoint.
4. Store API keys securely in .env file
   ```sh
   openAI.apiKey=<gpt_key>
   user.token=<auth_token>
   ```

## Hiding API Keys from Git
To keep your API keys secure:
1. **Use Environment Variables**:
   - Store API keys in environment variables instead of hardcoding them in your code.
   - Example in Python:
     ```python
     import os
     OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
     ```

2. **Use a `.env` File** (for local development):
   - Create a `.env` file:
     ```sh
     OPENAI_API_KEY=your_api_key_here
     ```
   - Use `python-dotenv` in Python to load it:
     ```python
     from dotenv import load_dotenv
     load_dotenv()
     ```

3. **Add `.env` to `.gitignore`**:
   - Open or create a `.gitignore` file and add:
     ```
     .env
     config.json
     *.secrets
     ```
   - This prevents sensitive files from being committed to Git.
