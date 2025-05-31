## Hubitat Fun Api Responses App
 * Idioms, Jokes, Trivia, Poems, Quotes, Riddles, Cat-Facts, Zen-Facts, Advice, Useless-Facts, Dad-Facts
 
 ---
 
This application generates random responses suitable for TTS on capability 'SpeechSynthesis' compatible devices (e.g., Amazon, Google, Sonos, Ikea, etc.).  

To obtain an API string for the categories indicated below, you must have a free account on [APILeague.com](https://apileague.com/). You may wish to purchase their paid plans, which allow you to receive more responses (tokens) per day.

The 'Idiom' category does not require an API account or API string. The **Idioms** category is powered by a JSON file (over 1,350 entries).

<center>

| Category  | API Required  | Api Website  |  Responses |
|---|:---:|:---:|:---:|
| Jokes | Yes |  [ApiLeague.com](https://apileague.com/) | Joke |
| Trivia | Yes |  [ApiLeague.com](https://apileague.com/) | Trivia |
| Poem | Yes |  [ApiLeague.com](https://apileague.com/) | Title, Author, Poem|
| Quotes | Yes |  [ApiLeague.com](https://apileague.com/) | Author, Quote |
| Riddles | Yes |  [ApiLeague.com](https://apileague.com/) | Question, Answer|
| Idioms | No |[GitHub Repo](https://github.com/KurtSanders/Hubitat-Fun-Api-Responses/tree/main/data/production) | Phrase, Definition |
| Dad-Jokes | No |  icanhazdadjoke.com |Jokes |
| Useless-facts| No |http://uselessfacts.jsph.pl  | text |
| Cat-Facts | No |  http://catfact.ninja | fact |
| Zen-Facts | No |  zenquotes.io | Author, Quote |
| Advice | No | api.adviceslip.com  | Slip |

</center>


These responses are not only enjoyable to hear but also educational, especially for the younger generation. Each separate device can trigger a random response with a push momentary button, switch, and/or a Hubitat rule, for example, when you wake up, go to bed, arrive home, have company over, turn a switch on or off, etc.

## Screen Captures

#### Application Interface

<img src="https://raw.githubusercontent.com/KurtSanders/Hubitat-Fun-Api-Responses/refs/heads/main/images/AppScreenCapture.jpg">

#### Devices
* Separate devices with a 'Momentary Push Button' and/or Command 'Refresh' to generate a new response

<img src="https://raw.githubusercontent.com/KurtSanders/Hubitat-Fun-Api-Responses/refs/heads/main/images/devces.jpeg">

## Installation

* Available via Hubitat Package Manager (HPM)
