# 🚀 AI Website Builder & Deployer

An AI-powered website builder built with **Java, Spring Boot, Spring AI, and Google Gemini** that aims to automate website creation and deployment using AI tool calling.

## 📌 Overview

AI Website Builder & Deployer is a backend application that uses an AI agent to generate static websites and manage project files through Java-based tools.

The goal is to simplify the website development workflow, from generating website code to publishing websites online using GitHub and GitHub Pages.

## ✨ Features

- 🤖 **AI-Powered Website Generation** — Generate static websites using Google Gemini.
- 🛠️ **AI Tool Calling** — Allow the AI agent to interact with Java tools to perform file and directory operations.
- 📁 **Project File Management** — Create separate directories for generated websites.
- 🌐 **Static Website Generation** — Generate HTML, CSS, and JavaScript files.
- 🔐 **Environment-Based Configuration** — Keep API keys and access tokens outside source code.
- 🐙 **GitHub Integration** — Integrate with GitHub APIs for repository and file management.
- 🚀 **Deployment Automation** — Develop an automated workflow for publishing generated websites through GitHub Pages.

## 🧰 Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend development |
| Spring Boot | Application framework |
| Spring AI | AI integration and tool calling |
| Google Gemini | Large language model |
| Maven | Dependency management and build |
| HTML5 | Website structure |
| CSS3 | Website styling |
| JavaScript | Website interactivity |
| Git & GitHub | Version control and repository hosting |
| GitHub Pages | Static website hosting |

## 🏗️ Architecture

1. The user provides a website creation request.
2. The AI agent processes the request using Google Gemini.
3. The agent invokes Java tools to create directories and website files.
4. The generated website contains HTML, CSS, and, when needed, JavaScript.
5. The GitHub integration can publish the generated files to a repository.
6. GitHub Pages can host the published static website.

## ⚙️ Prerequisites

Before running the application, install:

- JDK 21
- IntelliJ IDEA or another Java IDE
- Maven (or use the included Maven Wrapper)
- A Google Gemini API key
- A GitHub account and a token with the required permissions for publishing features

## 🔑 Environment Variables

Configure the following environment variables on your local machine:

| Variable | Description |
|---|---|
| `GOOGLE_API_KEY` | Google Gemini API key |
| `GITHUB_TOKEN` | GitHub access token for publishing operations |

Never hardcode API keys or tokens in the source code or commit actual secrets to GitHub.

### Application Configuration

In `src/main/resources/application.properties`, use environment-variable references:

```properties
spring.application.name=ai-website-builder
spring.ai.google.genai.api-key=${GOOGLE_API_KEY}
spring.ai.google.genai.chat.options.model=gemini-3.5-flash
github.token=${GITHUB_TOKEN}
```

## ▶️ Run Locally

1. Clone this repository:

   ```bash
   git clone https://github.com/YOUR_GITHUB_USERNAME/ai-website-builder.git
   ```

2. Open the project in IntelliJ IDEA.

3. Configure the required environment variables.

4. Reload the Maven project.

5. Run the Spring Boot main application class.

6. Submit a website generation request and inspect the generated files.

> Replace `YOUR_GITHUB_USERNAME` with your actual GitHub username after creating the repository.

## 📂 Project Structure

The project follows a standard Maven structure:

```text
ai-website-builder/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ... Java application and AI tools
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```


## ✅ Implemented Features

- [x] Automate GitHub repository creation.
- [x] Automate website file uploads.
- [x] Configure GitHub Pages deployment automatically.
- [x] Return published website URLs to the user.


Generated websites are stored separately according to the application's configured workspace path.
## 🛣️ Future Enhancements

- [ ] Improve generated website validation and error handling.
- [ ] Add deployment status tracking and clearer failure reporting.
- [ ] Build a polished web-based user interface.
- [ ] Add automated tests for generation and publishing workflows.


## 🔒 Security

- API keys are loaded from environment variables.
- Tokens should never be committed to version control.
- GitHub permissions should follow the principle of least privilege.
- Generated websites should be validated before publication.

## 👨‍💻 Author

**Onkar Dunakhe**

- GitHub: [github.com/onkardunakhe](https://github.com/onkardunakhe)
- LinkedIn: [linkedin.com/in/onkar-dunakhe](https://www.linkedin.com/in/onkar-dunakhe/)
- Portfolio:https://onkardunakhe.github.io/onkar-portfolio

---

⭐ If you find this project interesting, feel free to explore the repository and follow its development.
