# Coatex

This is TOR based peer to peer, encrypted chatting application. The aim of this project is to bring chatting application on TOR, enable users to chat with each other with anonymity in mind. This application is on very initial stages yet it provides text messaging with media sharing capability.

## Getting Started

This application works on Tor binary. If you want to build it yourself, you need ```Android Studio``` for compilation. You need to install one ```BKS``` keystore into ```asset\certificates``` having name ```coatex.bks```. If you want to have ```keystore``` with different name, make sure its properly names in ```com.ivor.coatex.tor.FileServer``` file. This keystore is necessary for running https server on Android.

### Prerequisites

This source code is ready to build and use but if you want to add some more features you can clone and add your features too. This is very initial code base, it might contain bugs. It includes pre compiled ```Tor 0.4.2.6``` binary. You can compile your own version of ```Tor``` and include in this project yourself.

## Built With

I have used many open-source libraries. These libraries can be found in app.gradle file.

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## License

This project is licensed under the GPL v3 License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

As COVID-19 pandemic occurred in 2020, This was the best use of time while quarantined at home. I developed this application because I didn't find any application that was mature enough to provide encrypted messaging over tor with media support. There is one application [Chat.onion](https://github.com/onionApps/Chat.onion) on github, but this is 4 years old application. Moreover this application is very hard to find on Playstore. Chat.Onion is the inspiration for the development of this application. Any bugfixes, security audit, code suggestion are very necessary for mature and secure application.
