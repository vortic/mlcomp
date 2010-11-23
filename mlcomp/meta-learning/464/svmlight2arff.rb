#!/usr/bin/ruby
require 'erb'
arff_template ='@relation <%= filename.split("/").last %>
<% (1..numatts).each do |att|%>
@attribute att_<%= att %> numeric<% end %>
@attribute class {<%=labels.uniq.join(",")%>}

@data<% rows.each_with_index do |row,ind| %>
{<%= row.keys.sort.map {|k| "#{k-1} #{row[k]}"}.join(",") %>,<%= "#{numatts} #{labels[ind]}"%>}<% end %>'

filename = ARGV.shift
outfile = ARGV.shift || filename + ".arff"

rows = []
labels = []
File.open(filename) do |file|
  file.each_line do |line|
    arr = line.chomp.split(" ")
    label = "class" + arr.shift
    labels << label
    row = {}
    arr.each do |pair|
      k,v = pair.split(":")
      row[k.to_i] = v.to_f
    end
    rows << row
  end
end

numatts = rows.map{|x| x.keys.max}.max
File.open(outfile,"w") do |outfile|
  outfile.write ERB.new(arff_template).result(binding)
end