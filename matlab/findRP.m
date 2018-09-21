%% Funtion Find RP
%
% Input: Stream= [dir[1]* rssi[80] magnetic_vector[3]]
%        mode= 2 both RSSI and MV are used to create a single classifier
%              1 both RSSI and MV are used to create a separated classifier
%
% Output: rssi_c        classifier on RSSI measurements on clustered data
%         mv_c          classifier on  MV  measurements on clustered data
%         cat_rssi_c    classifier on RSSI measurements on categorized data 
%         cat_mv_c      classifier on  MV  measurements on categorized data 
%         sub_rssi      classifier on sub RSSI  measurements on raw data
%         sub_mv        classifier on sub  MV   measurements on raw data
%         sub_cat_rssi  classifier on sub RSSI  measurements on categorized data
%         sub_cat_mv    classifier on sub  MV   measurements on categorized data
function [rssi_c,mv_c,cat_rssi_c,cat_mv_c,sub_rssi,sub_mv,sub_cat_rssi,sub_cat_mv]=findRP(data_stream,mode)
persistent vs;
persistent knn;
persistent svm_cluster;
persistent knn_mv;
persistent svm_cluster_mv;
persistent features;
persistent dir;
persistent old_flag;
persistent old_mode;
persistent direction;
if(length(data_stream)>83)
    flag=1;
else 
    flag=2;
end
if(isempty(vs) || isempty(old_flag) || old_flag~=flag || old_mode~=mode)    
    old_flag=flag;
    old_mode=mode;
    switch mode
        case 1 % DEVIDED 
            if (flag==1) % Direction included 
                load('vs.mat','dir');
                vs=dir;
                clear dir;
                direction=1;
            else         % Direction not included
                vs=load('vs.mat','no_dir');
                vs=no_dir;
                clear no_dir;    
                direction=[];
            end
            case 2 % Not DEVIDED
            if (flag==1) % Direction included
                load('vs.mat','all_dir');
                vs=all_dir;
                clear all_dir;
                direction=1;
            else         % Direction not included
                vs=load('vs.mat','all_no_dir');
                vs=all_no_dir;
                clear all_no_dir;    
                direction=[];
            end            
    end
end

%% Differentiate between Direction and not Direction
if(length(data_stream)>83) % Direction is passed
    direction=data_stream(1);
    rssi=data_stream(2:81);
    magnetic_vector=data_stream(82:84);
else % Direction is not passed
    direction=[];
    rssi=data_stream(1:80);
    magnetic_vector=data_stream(81:83);
end
    
%% Categorize data
stream=[direction rssi magnetic_vector];
cat_rssi=categorize_rssi(rssi,vs.features_rssi);
cat_magnetic_vector=categorize_mv(magnetic_vector,vs.features_mv);
cat_stream=[direction cat_rssi cat_magnetic_vector];

%% Classify
if(mode==1)
    rssi_c=predict(vs.svm_rssi_cluster{1},stream(1:end-3));
    mv_c=predict(vs.svm_mv_cluster{1},stream([direction end-2:end]));

    cat_rssi_c=predict(vs.svm_rssi_cluster{2},cat_stream(1:end-3));
    cat_mv_c=predict(vs.svm_mv_cluster{2},stream([direction end-2:end]));

    sub_rssi=predict(vs.sub_classifier_rssi_un{rssi_c},stream(1:end-3));
    sub_mv=predict(vs.sub_classifier_mv{mv_c},stream([direction end-2:end]));

    sub_cat_rssi=predict(vs.sub_classifier_rssi_un{cat_rssi_c},cat_stream(1:end-3));
    sub_cat_mv=predict(vs.sub_classifier_mv_un{cat_mv_c},stream([direction end-2:end]));
else
    rssi_c=predict(vs.svm_all_cluster{1},stream(1:end));
    
    cat_rssi_c=predict(vs.svm_all_cluster{2},cat_stream(1:end));
    
    sub_rssi=predict(vs.sub_classifier_all{rssi_c},stream(1:end));
    
    sub_cat_rssi=predict(vs.sub_classifier_all_un{cat_rssi_c},cat_stream(1:end));
    
    mv_c=-1;
    cat_mv_c=-1;
    sub_mv=-1;
    sub_cat_mv=-1;
end


end


function rssi= categorize_rssi(rssi,features)
[r,c]=size(features);
for i=1:r
     min=features(i,2);
     max=features(i,4);
     a=find(rssi<=max & rssi>=min);
     rssi(a)=i;
end
end


function mv= categorize_mv(mv,features)

    for j=1:3
        [r,c]=size(features{j});
        for i=1:r
             min=features{j}(i,2);
             max=features{j}(i,4);
             a=find(mv(j)<=max & mv(j)>=min);
             if(~isempty(a))
                mv(j)=i;
             end
        end
    end
end