using System;
using System.Speech.Recognition;

namespace SpeechHelper
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                // Create an in-process speech recognizer.
                // Using a 'using' statement ensures resources are released.
                using (SpeechRecognitionEngine recognizer = new SpeechRecognitionEngine())
                {
                    // Create and load a dictation grammar.
                    // This grammar allows the recognizer to recognize any spoken words.
                    recognizer.LoadGrammar(new DictationGrammar());

                    // Add a handler for the SpeechRecognized event.
                    recognizer.SpeechRecognized += new EventHandler<SpeechRecognizedEventArgs>(recognizer_SpeechRecognized);

                    // Configure the recognizer to use the default audio device.
                    recognizer.SetInputToDefaultAudioDevice();

                    // Start asynchronous, continuous recognition.
                    // 'RecognizeMode.Multiple' means it will keep listening after recognizing something.
                    recognizer.RecognizeAsync(RecognizeMode.Multiple);

                    // Keep the console application running.
                    // The Java application will terminate this process when it's done.
                    Console.WriteLine("Speech helper started. Listening for speech...");
                    Console.ReadLine();
                }
            }
            catch (Exception ex)
            {
                // Write any errors to the console so they can potentially be read by the Java app's error stream.
                Console.WriteLine("An error occurred in the speech helper: " + ex.Message);
            }
        }

        // Handle the SpeechRecognized event.
        private static void recognizer_SpeechRecognized(object sender, SpeechRecognizedEventArgs e)
        {
            if (e.Result != null && !string.IsNullOrEmpty(e.Result.Text))
            {
                // Write the recognized text to the standard output.
                // This is how we send the text to our Java application.
                Console.WriteLine(e.Result.Text);

                // Immediately flush the output stream to ensure the Java app receives it in real-time.
                Console.Out.Flush();
            }
        }
    }
}
