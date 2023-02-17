# Creating a static documentation

Each version should include a matching documentation.

Currently, the documentation is built from the github-wiki using [MkDocs](https://www.mkdocs.org/).

## Install mkdocs
Install mkdocs using

```pip install mkdocs```

Additionally, you need [material for mkdocs](https://squidfunk.github.io/mkdocs-material/). Install material for mkdocs using

```pip install mkdocs-material```

## Building the documentation

To build the documentation start the script &ldquo;build_docs.py&rdquo; located in the project&apos;s &ldquo;docs&rdquo; directory. You need Python version 3.0 or higher with mkdocs installed to do so. 

```&hellip;>python build_docs.py```


The script will download the wiki, build the docs and start the server.

The documentation is then available at http://127.0.0.1:8000/

To create a static documentation, execute:

```&hellip;>mkdocs build```


