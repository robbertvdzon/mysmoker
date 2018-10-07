#!/bin/bash
set -eu

aws s3 sync frontend s3://mysmoker.vdzon.com --delete
