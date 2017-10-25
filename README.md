See it in action: https://www.youtube.com/watch?v=wXnT1VZnRcA

## Inspiration
Nafisa had an idea of creating binding contracts for verbal agreements, to allow people to achieve more by being held to their word. Oliver comes from Mexico, where most transactions for the average person use cash so many people have no credit histories, and cannot take out loans. We realized that by combining Nafisa's idea with an issue that was important to Oliver, we could create a robust contract system which could be used to generate credit scores.

## What it does
DubCredit allows users to create binding contracts for verbal agreements. DubCredit creates contracts for anything ranging from $5 loaned to a friend to $5000 loaned by a bank or investor to an entrepreneur starting a business. Similar to a credit score in the United States, users can build up credit histories by honoring verbal agreements and making on-time payments. Strong credit histories will create a high DubCredit Score©, and defaults and late payments lower the DubCredit Score©.

When making contracts with others, the user can see the other person's DubCredit Score© to determine if they are willing to make a given contract with that person. DubCredit will increase the willingness to lend in societies where credit scores are not readily available, help business owners mitigate risk, and promote investments in new ventures.

## How we built it
DubCredit is an Android app programmed in Java. We used Firebase authentication to create user accounts and data to store on a Firebase database. The app converts verbal agreements to contracts using Microsoft Cognitive Services to translate voice to text and infer meaning from statements. We use the Speaker Recognition API to ensure that contracts are made only by the intended users and Bind Speech API with LUIS to determine what contracts to make. We use Softheon Payment API to allow users to electronically transfer money but also feature a way to verify in-person cash payments.

## Social Impact of DubCredit
DubCredit brings the tools of binding contracts and credit (including credit scores and credit histories) to people all around the world. Factors like [Corruption Perceptions Index](https://www.transparency.org/news/feature/corruption_perceptions_index_2016#table) dissuade investors due to the lack of reliable access to binding contracts free of bribery and extortion. By offering binding contracts, DubCredit helps countries become more appealing to investment, from the inside and from abroad. In particular, the everyday citizen can have the confidence to make deals within their communities.

Traditionally, only around [45%](http://cardtrak.com/data/96463/mexican-consumers-finally-get-credit-scores-deserve) of Mexican consumers can be given a credit score. Over 10 million Mexicans do not have enough credit history to even determine a risk score. In rural Mexico, [only 6 out of 100](http://www.worldbank.org/en/news/feature/2012/12/12/mexico-more-than-half-of-households-do-not-have-bank-account) people have a bank account. This situation is the same in many countries throughout the world; there is a strong link between access to credit and new business ventures within a country. Reliable credit scores lead to [increased lending and economic growth](http://partners4prosperity.com/understanding-the-impact-of-credit-in-the-economy). DubCredit users can immediately access the DubCredit Score© of users they wish to make deals with, and determine how much they are willing to risk with the deal. That way, people will know that they are dealing with someone trustworthy who won't lose or take their money, or that they are taking a substantial risk in dealing with someone. Through our system, we hope to foster peer to peer lending within communities to promote economic growth, entrepreneurship, and small business ventures.

## Challenges we ran into
Half of our team was unfamiliar with Android development, so we had a lot of work to do to set up our environments correctly. The Microsoft Cognitive Services and Firebase APIs were equally unfamiliar, and a lot of our time was spent integrating them into an Android app.

## Accomplishments that we're proud of
We believe that our app could be implemented in the real world, to help real communities. We are proud of making a functioning app and an excellent design.

## What we learned
Using new technologies is hard! We might have gone with a web app in the future in order to make faster and further progress.

## What's next for DubCredit
1. Develop all the features of our app and add more contract options.
2. Perform user research to determine how to adjust it for target markets
