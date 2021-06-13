#!/bin/bash

sbt '~ ++3.0.0 test ; ++2.13.6 testBase/test ; spi/test'

