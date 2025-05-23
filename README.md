AI Resume Analyzer
The AI Resume Analyzer is a Java Swing application that helps job seekers optimize their resumes. It uses the Cohere AI API for various analyses and includes a chatbot.



Features

Resume Upload (PDF): Upload PDF resumes for analysis.

ATS Compatibility: Get a numerical ATS score out of 100, plus details on format, sections, skills, and keyword matches.

Interview Probability: Estimates the chance of getting an interview.

Grammar Check: Identifies errors and suggests improvements.

Chatbot: Ask resume-related questions for AI-powered responses.

Resume Comparison: Compare multiple resumes based on ATS scores.

Download Analysis: Save the full report as a PDF.



Technologies

Java Swing: For the user interface.

Apache PDFBox: Extracts text from PDFs.

Cohere API: Powers AI analysis, grammar checks, and the chatbot.



Getting Started

Prerequisites

Java Development Kit (JDK) 8+

Apache PDFBox and JSON libraries (download JARs and add to classpath).



Run

Clone the repository.

Compile and run using javac and java, including the PDFBox and JSON JARs in the classpath.



How to Use

Upload: Click "Upload Resumes (PDF)".

Analyze: Click "Analyze Resume" for the latest uploaded file's analysis.

Compare: Upload multiple resumes and click "Compare Resumes" to rank them by ATS score.

Chat: Type in the chatbot field and click "Send".

Download: Click "Download Analysis (as PDF)".

