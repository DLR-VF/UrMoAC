# How To Release

## Building a release
* check whether docs are up to date
    * build docs as described at [Static Documentation](./DeveloperStaticDocumentation.md)
    * disable incomplete docs
    * compress it as &ldquo;site.zip&rdquo;

* check whether the code is clean
    * The methods should be documented
    * TODOs / &ldquo;!!!&rdquo; should be removed &mdash; maybe formulating them as github-Issues is an option
    * formatting should be updated

* build release zips
    * Export UrMoAC to a jar with included or adjacent libraries it uses

* build the release on github
    * the tag should be the current semantic version (***&lt;VERSION&gt;***)
    * the name should be &ldquo;**UrMoAC-*&lt;VERSION&gt;***&rdquo;
    * add some notes about what has been changed (see below)
    * add the compressed docs to the release

## Contents of the release text

* short note / welcome
* [ChangeLog](./ChangeLog.md)
* Information about using the documentation
* greetings


