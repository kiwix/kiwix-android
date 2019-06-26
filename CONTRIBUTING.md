# Contributing Guidelines

We love improvements to our tools! There are a few key ways you can help us improve our projects:

### Submitting Feedback, Requests, and Bugs

Our process for submitting feedback, feature requests, and reporting bugs usually begins by discussion on [our chat](http://wiki.kiwix.org/wiki/Communication#Chat) and, after initial clarification, through [GitHub issues](https://help.github.com/articles/about-issues/). Each project repository generally maintains its own set of issues:

        https://github.com/kiwix/<repository-name>/issues

Some projects have additional templates or sets of questions for each issue, which you will be prompted to fill out when creating one.

Issues that span multiple projects or are about coordinating how we work overall are in the [Overview Issue Tracker](https://github.com/kiwix/overview/issues).

### Submitting Code and Documentation Changes

We still do not have [project guidelines](./guidelines/project_guidelines.md) for all of the projects hosted in our [GitHub Organization](https://github.com/kiwix), which new repositories should follow during their creation.

Our process for accepting changes operates by [Pull Request (PR)](https://help.github.com/articles/about-pull-requests/) and has a few steps:

1.  If you haven't submitted anything before, and you aren't (yet!) a member of our organization, **fork and clone** the repo:

        $ git clone git@github.com:<your-username>/<repository-name>.git

    Organization members should clone the upstream repo, instead of working from a personal fork:

        $ git clone git@github.com:kiwix/<repository-name>.git

1.  Create a **new branch** for the changes you want to work on. Choose a topic for your branch name that reflects the change:

        $ git checkout -b <branch-name>

1.  **Create or modify the files** with your changes. If you want to show other people work that isn't ready to merge in, commit your changes then create a pull request (PR) with _WIP_ or _Work In Progress_ in the title.

        https://github.com/kiwix/<repository-name>/pull/new/master

1.  Once your changes are ready for final review, commit your changes then modify or **create your pull request (PR)**, assign as a reviewer or ping (using "`@<username>`") a Lieutenant (someone able to merge in PRs) active on the project (all Lieutenants can be pinged via `@kiwix/lieutenants`)

1.  Allow others sufficient **time for review and comments** before merging. We make use of GitHub's review feature to comment in-line on PRs when possible. There may be some fixes or adjustments you'll have to make based on feedback.

1.  Once you have integrated comments, or waited for feedback, a Lieutenant should merge your changes in!

### Branching

Our branching strategy is based on [this article](https://nvie.com/posts/a-successful-git-branching-model/) which we suggest you read.

+ **master** a history of releases, once merged to from develop and tagged we create a release on the play store & GitHub releases.
+ **develop**  the actively worked on next release of the app, what we branch off of while working on new features and what we merge into upon feature completion
+ **feature/** or feature/\<username\>/ any branch under this directory is an actively developed feature, feature branches culminate in a PR, are merged and deleted. Typically a feature branch is off of develop and into develop but in rare scenarios if there is an issue in production a branch may be made off master to fix this issue, this type of feature branch must be merged to develop and master before being deleted.

All branches should have distinct history and should be visually easy to follow, for this reason only preform merge commits when merging code either by PR or when synchronising.

Rebasing should be avoided.

_These guidelines are based on [Tools for Government Data Archiving](https://github.com/edgi-govdata-archiving/overview/blob/master/CONTRIBUTING.md)'s._
