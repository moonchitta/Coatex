# Coatex

This is TOR based peer to peer, encrypted chatting application. The aim of this project is to bring chatting application on TOR, enable users to chat with each other with anonymity in mind. This application is on very initial stages yet it provides text messaging with media sharing capability.

## Screenshots

![](https://raw.githubusercontent.com/moonchitta/Coatex/master/Screenshots/Screenshot_1587189547.png) <!-- .element height="50%" width="50%" -->
![](https://raw.githubusercontent.com/moonchitta/Coatex/master/Screenshots/Screenshot_1587189578.png) <!-- .element height="50%" width="50%" -->
![](https://raw.githubusercontent.com/moonchitta/Coatex/master/Screenshots/Screenshot_1587189586.png) <!-- .element height="50%" width="50%" -->
![](https://raw.githubusercontent.com/moonchitta/Coatex/master/Screenshots/Screenshot_1587189593.png) <!-- .element height="50%" width="50%" -->
![](https://raw.githubusercontent.com/moonchitta/Coatex/master/Screenshots/Screenshot_1587189645.png) <!-- .element height="50%" width="50%" -->
![](https://raw.githubusercontent.com/moonchitta/Coatex/master/Screenshots/Screenshot_1587189688.png) <!-- .element height="50%" width="50%" -->

## Technical Details

This application creates hidden service using hostname as ID of the application. One has to keep in mind that, it does not include server for routing of messages, all messages are sent directly to other users. There is no online or typing status for the application, but it does indicate that message has been delivered to the user. Moreover, media sharing is slow as compared to other messaging application, because everything runs over Tor. Sharing large files are possible, but you have to rely on the network speed of tor nodes.

## Features

* Encrypted chatting
* HTTPS based, password oriented file sharing
* Dark Mode (can be changed from settings)
* Add requests
* Request notification
* Message notification
* Save media to external storage
* Share media from other applications
* Display picture
* Swipe to tag message
* Message tag
* Media message tag
* Encrypted ID backup and restore

## Getting Started

This application works on Tor binary. If you want to build it yourself, you need ```Android Studio``` for compilation. You need to install one ```BKS``` keystore into ```asset\certificates``` having name ```coatex.bks```. If you want to have ```keystore``` with different name, make sure same name in ```com.ivor.coatex.tor.FileServer``` file. This keystore is necessary for running https server on Android.

### Prerequisites

This source code is ready to build and use but if you want to add some more features you can clone and add your features too. This is very initial code base, it might contain bugs. It includes pre compiled ```Tor 0.4.2.6``` binary. You can compile your own version of ```Tor``` and include in this project yourself.

## Built With

I have used many open-source libraries. These libraries can be found in app.gradle file.

## License

This project is licensed under the GPL v3 License - see the [LICENSE.md](https://raw.githubusercontent.com/moonchitta/Coatex/master/LICENSE) file for details

## Credits

There many open-source libraries used in this application. Without those libraries it wasn't possible to develop this application in a month. Moreover some icons have been taken from [flaticon](http://www.flaticon.com/). Icons made by [Roundicons](https://www.flaticon.com/authors/roundicons) from [flaticon](http://www.flaticon.com/) are used in this application.

## Acknowledgments

As COVID-19 pandemic occurred in 2020, This was the best use of time while quarantined at home. I developed this application because I didn't find any application that was mature enough to provide encrypted messaging over tor with media support. There is one application [Chat.onion](https://github.com/onionApps/Chat.onion) on github, but this is 4 years old application. Moreover this application is very hard to find on Playstore. Chat.Onion is the inspiration for the development of this application. Any bugfixes, security audit, code suggestions are very necessary for making mature and secure application.
